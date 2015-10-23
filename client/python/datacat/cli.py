#!/usr/bin/python

import argparse
import json
import logging
import sys
from .auth import auth_from_config
from .client import Client
from .config import config_from_file
from .error import DcException, DcClientException, DcRequestException
from .model import Container, Dataset, Group


def build_argparser():
    parser = argparse.ArgumentParser(description="Python CLI for Data Catalog RESTful interfaces")
    parser.add_argument('-U', '--base-url', help="Override base URL for client", action="store")
    parser.add_argument('-D', '--domain', "--experiment", help="Set domain (experiment) for requests", default="srs")
    parser.add_argument('-M', '--mode', help="Set server mode", choices=("dev", "prod"), default="prod")
    parser.add_argument('-d', '--debug', help="Debug Level", action="store_true")
    sub = parser.add_subparsers(help="Command help")
    
    def add_search(subparsers):
        parser_search = subparsers.add_parser("search", help="Search command help",
                                              formatter_class=argparse.RawTextHelpFormatter)
        parser_search.add_argument('path', help="Container Search path (or pattern)")
        parser_search.add_argument('-v', '--version', dest="versionId",
                                   help="Version to query (default equivalent to 'current' for latest version)")
        parser_search.add_argument('-s', '--site', dest="site",
                                   help="Site to query (default equivalent to 'canonical' for master site)")
        parser_search.add_argument('-q', '--query', dest="query", help="Query String for datasets")
        parser_search.add_argument('--show', nargs="*", metavar="FIELD", help="List of columns to return")
        parser_search.add_argument('--sort', nargs="*", metavar="FIELD",
                                   help="Fields and metadata to sort by. \n"
                                   "If sorting in descending order, \n"
                                   "append a dash to the end of the field. \n\n"
                                   "Examples: \n"
                                   " --sort nRun- nEvents\n"
                                   " --sort nEvents+ nRun-")
        parser_search.set_defaults(command="search")
    
    def add_path(subparsers):
        cmd = "path"
        parser_path = subparsers.add_parser(cmd, help="search help")
        parser_path.add_argument('path', help="Path to stat")
        parser_path.add_argument('-v', '--version', dest="versionId",
                                 help="Version to query (default equivalent to 'current' for latest version)")
        parser_path.add_argument('-s', '--site', dest="site",
                                 help="Site to query (default equivalent to 'canonical' for master site)")
        parser_path.add_argument('-S', '--stat', dest="stat",
                                 help="Type of stat to return (for containers)", choices=("none", "basic", "dataset"))
        parser_path.set_defaults(command=cmd)
    
    def add_children(subparsers):
        cmd = "children"
        parser_children = subparsers.add_parser(cmd, help="Help with the children command")
        parser_children.add_argument('path', help="Container to query")
        parser_children.add_argument('-v', '--version', dest="versionId",
                                     help="Version to query (default equivalent to 'current' for latest version)")
        parser_children.add_argument('-s', '--site', dest="site",
                                     help="Site to query (default equivalent to 'canonical' for master site)")
        parser_children.add_argument('-S', '--stat', dest="stat",
                                     help="Type of stat to return", choices=("none", "basic", "dataset"))
        parser_children.set_defaults(command=cmd)

    def add_mkds(subparsers):
        cmd = "mkds"
        parser_children = subparsers.add_parser(cmd, help="Making a dataset")
        parser_children.add_argument('path', help="Dataset path")
        parser_children.add_argument('name', help="Dataset name")
        parser_children.add_argument('dataType', help="Dataset data type")
        parser_children.add_argument('fileFormat', help="Dataset file format")
        parser_children.add_argument('versionId',
                                     help="Version to query (default equivalent to 'current' for latest version)")
        parser_children.add_argument('site', help="Location site")
        parser_children.add_argument('resource', help="Location resource")
        parser_children.set_defaults(command=cmd)

    def add_rmds(subparsers):
        cmd = "rmds"
        parser_children = subparsers.add_parser(cmd, help="Help with the children command")
        parser_children.add_argument('path', help="Path of dataset to remove")
        parser_children.set_defaults(command=cmd)

    def add_mkdir(subparsers):
        cmd = "mkdir"
        parser_children = subparsers.add_parser(cmd, help="Help with the children command")
        parser_children.add_argument('path', help="Container path")
        parser_children.add_argument('type', help="Container Type (defaults to folder)", choices=("folder", "group"))
        parser_children.set_defaults(command=cmd)

    def add_rmdir(subparsers):
        cmd = "rmdir"
        parser_children = subparsers.add_parser(cmd, help="Remove a container (group or folder)")
        parser_children.add_argument('path', help="Path of container to remove")
        parser_children.add_argument('type', help="Container Type (defaults to folder)", choices=("folder", "group"))
        parser_children.set_defaults(command=cmd)

    add_path(sub)
    add_children(sub)
    add_search(sub)
    add_mkds(sub)
    add_rmds(sub)
    add_mkdir(sub)
    add_rmdir(sub)
    
    return parser


def main():
    parser = build_argparser()
    args, extra = parser.parse_known_args()

    command = args.command
    target = args.__dict__.pop("path")
    params = args.__dict__ or {}

    config_file_path = args.config_file if hasattr("args", "config_file") else None
    config_section = args.config_section if hasattr("args", "config_section") else None

    config = config_from_file(config_file_path, config_section, args.domain, args.mode)
    url = args.base_url if hasattr(args, 'base_url') and args.base_url is not None else None
    if url:
        config["url"] = url
    if args.debug:
        config["debug"] = True
        logging.basicConfig(level=logging.DEBUG)
        requests_log = logging.getLogger("requests")
        requests_log.setLevel(logging.DEBUG)

    auth_strategy = auth_from_config(config)
    client = Client(auth_strategy=auth_strategy, **config)
    client_method = getattr(client, command)

    try:
        result = client_method(target, **params)
    except DcClientException as error:
        print error
        sys.exit(1)
    except DcRequestException as error:
        print error
        sys.exit(2)
    except DcException as error:
        print error
        sys.exit(3)

    if command == "search":
        format_search_results(result, args.show, args.sort)

    if command == "path":
        format_path_result(result)

    if command == "children":
        format_children(result, args.stat)


def format_search_results(results, show=None, sort=None):
    # noinspection PyShadowingNames
    def print_search_info(datasets, meta_names):
        print("\nListing locations...")
        print("Resource\tPath\t%s" % ("\t".join(meta_names)))
        for dataset in datasets:
            extra = ""
            if hasattr(dataset, "metadata"):
                extra = "\t".join([str(dataset.metadata.get(mn)) for mn in meta_names])
            if hasattr(dataset, "resource"):
                print("%s\t%s\t%s" % (dataset.resource, dataset.path, extra))
            elif hasattr(dataset, "locations"):
                for location in dataset.locations:
                    print("%s\t%s\t%s" % (location.resource, dataset.path, extra))

    meta_names = []
    if show is not None:
        meta_names.extend(show)
    if sort is not None:
        s = []
        s.extend(sort)
        for i in s:
            if i[-1] in ("+", "-"):
                meta_names.append(i[0:-1])
            else:
                meta_names.append(i)
    meta_names = set(meta_names)
    print_search_info(results, meta_names)


def format_path_result(result):
    print maybe_hilite(result)
    print "Type: {}".format(type(result).__name__)
    print "Path: {}".format(result.path)
    if isinstance(result, Container):
        if hasattr(result, "stat"):
            print "Stat:"
            for name, value in result.stat.items():
                if "_type" not in name:
                    print "    {}: {}".format(name, value)

    if hasattr(result, "versionMetadata"):
        print "Version Metadata:\n    ",
        print json.dumps(result.versionMetadata, sort_keys=False, indent=4)

    if hasattr(result, "metadata"):
        print "Metadata:"
        mdstr = json.dumps(result.metadata, sort_keys=False, indent=4)
        print "    " + mdstr.replace("\n", "\n    ")


def maybe_hilite(result):
    name = result.name if isinstance(result, Dataset) else result.name + "/"
    attr = []
    if isinstance(result, Group):
        attr.append('35')
    elif isinstance(result, Container):
        attr.append('34')
    else:
        attr.append('30')
    return '\x1b[%sm%s\x1b[0m' % (';'.join(attr), name) if sys.stdout.isatty() else name


def format_children(results, stat):
    for result in results:
        if stat:
            print "#"*32
            format_path_result(result)
        else:
            print maybe_hilite(result)

if __name__ == '__main__':
    main()
