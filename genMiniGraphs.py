

from torch.utils.data import Dataset, DataLoader
import torch
from transformers import AutoTokenizer, AutoModel
from torch_geometric.data import HeteroData
from typing import Dict, List, Union

import torch
import torch.nn.functional as F
from torch import nn

import torch_geometric.transforms as T
from torch_geometric.nn import GCNConv

from torch_geometric.data import HeteroData
import json


tokenizer = AutoTokenizer.from_pretrained("microsoft/codebert-base")



def getAllGraph():
    fDirMap = {}
    for i in range(0, 1573):
        testDir = f"test{i}"
        fDir = f"data/test{i}"
        graphPath = f"{fDir}/graph1.json"

        with open(fDir + "/info.json", "r") as f:
            info = json.load(f)

        graph = None
        with open(graphPath, "r") as f:
            graph = json.load(f)

            fDirMap[testDir] = graph
        print(fDir)
    return fDirMap



def toBidirectional(graph, fDir):
    for node in graph:
        node["fDir"] = fDir
        index = node["nodeIndex"]

        for e in node["cfgs"]:
            if index not in graph[e]["cfgs"]:
                graph[e]["cfgs"].append(index)

        for e in node["dfgs"]:
            if index not in graph[e]["dfgs"]:
                graph[e]["dfgs"].append(index)

        for e in node["fieldParents"]:
            if index not in graph[e]["fieldParents"]:
                graph[e]["fieldParents"].append(index)

        for e in node["methodParents"]:
            if index not in graph[e]["methodParents"]:
                graph[e]["methodParents"].append(index)



def clone(node):
    cnode = {}
    cnode["cfgs"] = [e for e in node["cfgs"]]
    cnode["dfgs"] = [e for e in node["dfgs"]]
    cnode["fieldParents"] = [e for e in node["fieldParents"]]
    cnode["methodParents"] = [e for e in node["methodParents"]]
    cnode["commits"] = [cid for cid in node["commits"]]

    cnode["code"] = node["code"]
    cnode["fName"] = node["fName"]
    cnode["isDel"] = node["isDel"]
    cnode["lineBeg"] = node["lineBeg"]
    cnode["lineEnd"] = node["lineEnd"]
    cnode["lineMapIndex"] = node["lineMapIndex"]
    cnode["nodeIndex"] = node["nodeIndex"]
    cnode["rootcause"] = node["rootcause"]
    cnode["fDir"] = node["fDir"]
    return cnode



def dfs(index, depth, graph, newGraph, visited):
    if depth >= 2 or (index in visited) or len(visited) > 8:
        return

    newGraph.append(clone(graph[index]))
    visited.add(index)
    curNode = graph[index]

    for e in curNode["cfgs"][:3]:
        dfs(e, depth + 1, graph, newGraph, visited)

    for e in curNode["dfgs"][:1]:
        dfs(e, depth + 1, graph, newGraph, visited)

    for e in curNode["fieldParents"][:1]:
        dfs(e, depth + 1, graph, newGraph, visited)

    for e in curNode["methodParents"][:1]:
        dfs(e, depth + 1, graph, newGraph, visited)

    if curNode["lineMapIndex"] != -1:
        dfs(curNode["lineMapIndex"], depth + 1, graph, newGraph, visited)



def adjustIndex(newGraph):
    delIndexMap = {}
    addIndexMap = {}

    delCnt = 0
    addCnt = 0

    for node in newGraph:
        if node["isDel"]:
            delIndexMap[node["nodeIndex"]] = delCnt
            delCnt = delCnt + 1
        else:
            addIndexMap[node["nodeIndex"]] = addCnt
            addCnt = addCnt + 1

    indexMap = None

    for node in newGraph:
        if node["isDel"]:
            indexMap = delIndexMap
        else:
            indexMap = addIndexMap

        tmp = []
        for e in node["cfgs"]:
            if e in indexMap:
                e = indexMap[e]
                tmp.append(e)

        node["cfgs"] = tmp
        tmp = []

        for e in node["dfgs"]:
            if e in indexMap:
                e = indexMap[e]
                tmp.append(e)

        node["dfgs"] = tmp
        tmp = []

        for e in node["fieldParents"]:
            if e in indexMap:
                e = indexMap[e]
                tmp.append(e)

        node["fieldParents"] = tmp
        tmp = []

        for e in node["methodParents"]:
            if e in indexMap:
                e = indexMap[e]
                tmp.append(e)

        node["methodParents"] = tmp
        tmp = []

        if node["lineMapIndex"] != -1 and node["isDel"]:
            if node["lineMapIndex"] in addIndexMap:
                node["lineMapIndex"] = addIndexMap[node["lineMapIndex"]]
            else:
                node["lineMapIndex"] = -1
        elif node["lineMapIndex"] != -1 and not node["isDel"]:
            if node["lineMapIndex"] in delIndexMap:
                node["lineMapIndex"] = delIndexMap[node["lineMapIndex"]]
            else:
                node["lineMapIndex"] = -1

        node["nodeIndex"] = indexMap[node["nodeIndex"]]

    return newGraph



def genMiniGraphs(graph, fDir):
    allGraph = []
    toBidirectional(graph, fDir)
    for node in graph:
        if not node["isDel"]:
            continue
        node["fDir"] = fDir
        indexMap = {}
        newGraph = []
        visited = set()

        dfs(node["nodeIndex"], 0, graph, newGraph, visited)
        allGraph.append(adjustIndex(newGraph))

    return allGraph



def getAllMiniGraphs(fDirMap):
    allMiniGraphs = {}
    for fDir, graph in fDirMap.items():
        miniGraphs = genMiniGraphs(graph, fDir)
        allMiniGraphs[fDir] = miniGraphs
    return allMiniGraphs



def genAllMiniGraphs():
    fDirMap = getAllGraph()
    allMiniGraphs = getAllMiniGraphs(fDirMap)
    for fDir, miniGraphs in allMiniGraphs.items():
        for minig in miniGraphs:
            for node in minig:
                node["token_ids"] = tokenizer.encode_plus(
                    text=node["code"],
                    add_special_tokens=True,
                    max_length=64,
                    padding="max_length",
                )["input_ids"]

    with open("miniGraphs.json", "w") as f:
        json.dump(allMiniGraphs, f)
