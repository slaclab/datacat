#!/usr/bin/python

import sys
import pprint
import argparse

from model import unpack, DatacatObject
from client import Client, DcException
from config import *

def build_argparser():
    parser = argparse.ArgumentParser(description="Python CLI for Data Catalog RESTful interfaces")
    parser.add_argument('-U', '--base-url', help="Override base URL for client", action="store")
    parser.add_argument('-D', '--experiment', "--domain", help="Set experiment domain for requests")
    parser.add_argument('-f', '--format', dest="accept", default="json", help="Default format is JSON. JSON will attempted to be processed further")
    parser.add_argument('-r', '--show-request', action="store_true", dest="show_request",
                        help="Show request URL", default=False)
    parser.add_argument('-R', '--show-response', action="store_true", dest="show_response",
                        help="Attempt to show formatted response", default=False)
    parser.add_argument('-Rw', '--show-raw-response', action="store_true", dest="show_raw_response",
                        help="Show raw response", default=False)
    parser.add_argument('-rH', '--show-request-headers', action="store_true", dest="request_headers",
                        help="Show HTTP headers", default=False)
    parser.add_argument('-RH', '--show-response-headers', action="store_true", dest="response_headers",
                        help="Show HTTP headers", default=False)
    subparsers = parser.add_subparsers(help="Command help")
    
    def add_search(subparsers):
        parser_search = subparsers.add_parser("search", help="Search command help",
                                              formatter_class=argparse.RawTextHelpFormatter)
        parser_search.add_argument('path', help="Container Search path (or pattern)")
        parser_search.add_argument('-v', '--version', dest="version",
                                   help="Version to query (default equivalent to 'current' for latest version)")
        parser_search.add_argument('-s', '--site', dest="site",
                                   help="Site to query (default equivalent to 'canonical' for master site)")
        parser_search.add_argument('-q', '--query', dest="query", help="Query String for datasets")
        parser_search.add_argument('--show', nargs="*", metavar="FIELD", help="List of columns to return")
        parser_search.add_argument('--sort', nargs="*", metavar="FIELD", help=
        "Fields and metadata to sort by. \nIf sorting in descending order, \nappend a dash to the end of the field. " +
        "\n\nExamples: \n --sort nRun- nEvents\n --sort nEvents+ nRun-")
        parser_search.set_defaults(command="search")
    
    def add_path(subparsers):
        cmd = "path"
        parser_path = subparsers.add_parser(cmd, help="search help")
        parser_path.add_argument('path', help="Path to stat")
        parser_path.add_argument('-v', '--version', dest="version",
                                 help="Version to query (default equivalent to 'current' for latest version)")
        parser_path.add_argument('-s', '--site', dest="site",
                                 help="Site to query (default equivalent to 'canonical' for master site)")
        parser_path.add_argument('-S', '--stat', dest="stat",
                                 help="Type of stat to return (for containers)", choices=("none","basic","dataset"))
        parser_path.set_defaults(command=cmd)
    
    def add_children(subparsers):
        cmd = "children"
        parser_children = subparsers.add_parser(cmd, help="Help with the children command")
        parser_children.add_argument('path', help="Container to query")
        parser_children.add_argument('-v', '--version', dest="version",
                                     help="Version to query (default equivalent to 'current' for latest version)")
        parser_children.add_argument('-s', '--site', dest="site",
                                     help="Site to query (default equivalent to 'canonical' for master site)")
        parser_children.add_argument('-S', '--stat', dest="stat",
                                 help="Type of stat to return", choices=("none","basic","dataset"))
        parser_children.set_defaults(command=cmd)
    
    add_path(subparsers)
    add_children(subparsers)
    add_search(subparsers)
    
    return parser


def main():
    parser = build_argparser()
    args = parser.parse_args()

    command = args.command
    target = args.__dict__.pop("path")
    params = args.__dict__

    base_url = args.base_url if hasattr(args, 'base_url') and args.base_url is not None else CONFIG_URL("srs")
    client = Client(base_url)
    client_method = getattr(client, command)

    resp = None
    try:
        if len(params) > 0:
            resp = client_method(target, **params)
        else:
            resp = client_method(target)
    except DcException as error:
        if hasattr(error, "message"):
            print("Error occurred:\nMessage: %s" %(error["message"]))
            if "type" in error:
                print("Type: %s" %(error["type"]))
            if "cause" in error:
                print("Cause: %s" %(error["cause"]))
        else:
            # Should have content
            print(error.content)
        sys.exit(1)

    pp = pprint.PrettyPrinter(indent=2)

    if(args.accept != 'json'):
        sys.stderr.write("Response: %d\n" %(resp.status_code))
        if(args.accept == 'xml'):
            from xml.dom.minidom import parseString
            xml= parseString(resp.content)
            print(xml.toprettyxml())
        if(args.accept == 'txt'):
            print(resp.text)
        sys.exit(1)

    retObjects = []

    if(resp.status_code == 204):
        print("No Content")
        sys.exit(1)

    json = resp.json()

    dcObject = lambda d: '_type' in d and d['_type'].split("#")[0] in 'dataset group folder'.split(" ")

    if isinstance(json, dict):
        retObjects.append(unpack(json) if dcObject(json) else json)
    elif isinstance(json, list):
        for item in json:
            retObjects.append(unpack(item) if dcObject(item) else item)

    if args.show_response:
        print("Object Response:")
        pp.pprint([i if isinstance(i, DatacatObject) else i for i in retObjects])

    if command == "search":
        def print_search_info(datasets, metanames):
            print("\nListing locations...")
            print( "Resource\tPath\t%s" %("\t".join(metanames)))
            for dataset in datasets:
                extra = ""
                if hasattr(dataset, "metadata"):
                    extra = "\t".join([str(dataset.metadata.get(i)) for i in metanames])
                if hasattr(dataset, "resource"):
                    print( "%s\t%s\t%s" %(dataset.resource, dataset.path, extra))
                elif hasattr(dataset, "locations"):
                    for location in dataset.locations:
                        print( "%s\t%s\t%s" %(location.resource, dataset.path, extra))

        metanames = []
        if args.show is not None:
            metanames.extend(args.show)
        if args.sort is not None:
            s = []
            s.extend(args.sort)
            for i in s:
                if i[-1] in ("+", "-"):
                    metanames.append(i[0:-1])
                else:
                    metanames.append(i)
        metanames = set(metanames)
        print_search_info(retObjects, metanames)


if __name__ == '__main__':
    main()
