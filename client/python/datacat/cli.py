#!/usr/bin/python

import sys
import pprint
import argparse

from model import unpack
from client import Client
from config import *

def build_argparser():
    parser = argparse.ArgumentParser(description="Python CLI for Data Catalog RESTful interfaces")
    parser.add_argument('-U', '--base-url', help="Override base URL for client")
    parser.add_argument('-D', '--experiment', "--domain", help="Set experiment domain for requests")
    parser.add_argument('-R', '--show-raw-response', action="store_true", dest="response", help="Show raw response", default=False)
    parser.add_argument('-H', '--show-headers', action="store_true", dest="headers", help="Show HTTP headers", default=False)
    subparsers = parser.add_subparsers(help="Command help")
    
    def add_search(subparsers):
        parser_search = subparsers.add_parser("search", help="search help")
        parser_search.add_argument('path', help="Container Search path (or pattern)")
        parser_search.add_argument('-q', '--filter', dest="q", help="Query String for datasets")
        parser_search.add_argument('--show', nargs="*", metavar="FIELD", help="List of columns to return")
        parser_search.add_argument('--sort', nargs="*", metavar="FIELD", help='Fields and metadata to sort by. If sorting in descending order, append a dash to the end of the field. Examples: --sort nRun-, nEvents \n --sort nEvents+ nRun-')
        parser_search.set_defaults(command="search")
    
    def add_path(subparsers):
        cmd = "path"
        parser_path = subparsers.add_parser(cmd, help="search help")
        parser_path.add_argument('path', help="Path to stat")
        parser_path.set_defaults(command=cmd)
    
    def add_children(subparsers):
        cmd = "children"
        parser_children = subparsers.add_parser(cmd, help="Help with the children command")
        parser_children.add_argument('path', help="Container to query")
        parser_children.set_defaults(command=cmd)
    
    add_path(subparsers)
    add_children(subparsers)
    add_search(subparsers)
    
    return parser


def main():
    parser = build_argparser()
    args = parser.parse_args()

    command = args.command
    target = args.path
    params = args.__dict__

    base_url = args.base_url if hasattr(args, 'base_url') and args.base_url is not None else BASE_URL("srs")
    client = Client(base_url)
    client_method = getattr(client, command)

    if len(params) > 0:
        resp = client_method(target, **params)
    else:
        resp = client_method(target)

    pp = pprint.PrettyPrinter(indent=2)

    if(resp.status_code >= 400):
        if argparse.response:
            print resp.content

        if resp.status_code >= 500:
            print("Error processing request: %d" %resp.status_code)
            if argparse.response:
                print resp.content
            sys.exit(1)
        error = resp.json()
        print("Error occurred:\nMessage: %s" %(error["message"]))
        if "type" in error:
            print("Type: %s" %(error["type"]))
        if "cause" in error:
            print("Cause: %s" %(error["cause"]))
        sys.exit(1)

    retObjects = []

    json = resp.json()
    if isinstance(json, dict):

        retObjects.append(unpack(json))
    elif isinstance(json, list):
        for item in json:
            retObjects.append(unpack(item))



    if args.headers:
        print("Headers:")
        pp.pprint(resp.headers)

    if args.response:
        print("Object Response:")
        pp.pprint([i.raw for i in retObjects])


    if command == "search":
        def print_search_info(datasets, metanames):
            print("\nListing locations...")
            print( "Path\tFileSystemPath\t%s" %("\t".join(metanames)))
            for dataset in datasets:
                extra = ""
                if hasattr(dataset, "metadata"):
                    extra = "\t".join([str(dataset.metadata.get(i)) for i in metanames])
                print( "%s\t%s\t%s" %(dataset.path, dataset.fileSystemPath, extra))

        metanames = []
        if "show" in params:
            metanames.extend(params["show"])
        if "sort" in params:
            s = []
            s.extend(params["sort"])
        for i in s:
            if i[-1] in ("+", "-"):
                metanames.append(i[0:-1])
            else:
                metanames.append(i)
                metanames = set(metanames)

        print_search_info(retObjects, metanames)


if __name__ == '__main__':
    main()
