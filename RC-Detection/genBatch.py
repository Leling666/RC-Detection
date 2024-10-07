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
import random


class Batch:
    def __init__(self, pyg1, pyg2, del_index1, del_index2, probs):
        self.pyg1 = pyg1
        self.pyg2 = pyg2
        self.del_index1 = del_index1
        self.del_index2 = del_index2
        self.probs = probs


def adjustEdgeIndex(edge_index, Cnt1, Cnt2):
    edgeIndex1 = torch.add(edge_index[0], Cnt1)
    edgeIndex2 = torch.add(edge_index[1], Cnt2)
    edgeIndex1 = edgeIndex1.view(1, -1)
    edgeIndex2 = edgeIndex2.view(1, -1)

    return torch.cat((edgeIndex1, edgeIndex2), 0)


def combineGraph(pygs):
    data = HeteroData()

    data["add_node"].token_ids = torch.zeros((0, 64), dtype=torch.long)
    data["del_node"].token_ids = torch.zeros((0, 64), dtype=torch.long)

    data["add_node"].x = torch.zeros((0, 64), dtype=torch.long)
    data["del_node"].x = torch.zeros((0, 64), dtype=torch.long)

    data["del_node", "line_mapping", "add_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )
    data["del_node", "cdfg", "del_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )
    data["del_node", "ref", "del_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )

    data["add_node", "line_mapping", "del_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )
    data["add_node", "cdfg", "add_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )
    data["add_node", "ref", "add_node"].edge_index = torch.zeros(
        (2, 0), dtype=torch.long
    )

    del_indexes = []

    delCnt = 0
    addCnt = 0

    for pyg in pygs:
        del_indexes.append(delCnt)

        data["add_node"].token_ids = torch.cat(
            (data["add_node"].token_ids, pyg["add_node"].token_ids), 0
        )
        data["del_node"].token_ids = torch.cat(
            (data["del_node"].token_ids, pyg["del_node"].token_ids), 0
        )

        data["add_node"].x = torch.cat(
            (data["add_node"].x, pyg["add_node"].token_ids), 0
        )
        data["del_node"].x = torch.cat(
            (data["del_node"].x, pyg["del_node"].token_ids), 0
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["del_node", "line_mapping", "add_node"].edge_index, delCnt, addCnt
        )
        data["del_node", "line_mapping", "add_node"].edge_index = torch.cat(
            (
                data["del_node", "line_mapping", "add_node"].edge_index,
                adjust_edge_index,
            ),
            1,
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["del_node", "cdfg", "del_node"].edge_index, delCnt, delCnt
        )
        data["del_node", "cdfg", "del_node"].edge_index = torch.cat(
            (data["del_node", "cdfg", "del_node"].edge_index, adjust_edge_index), 1
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["del_node", "ref", "del_node"].edge_index, delCnt, delCnt
        )
        data["del_node", "ref", "del_node"].edge_index = torch.cat(
            (data["del_node", "ref", "del_node"].edge_index, adjust_edge_index), 1
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["add_node", "line_mapping", "del_node"].edge_index, addCnt, delCnt
        )
        data["add_node", "line_mapping", "del_node"].edge_index = torch.cat(
            (
                data["add_node", "line_mapping", "del_node"].edge_index,
                adjust_edge_index,
            ),
            1,
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["add_node", "cdfg", "add_node"].edge_index, addCnt, addCnt
        )
        data["add_node", "cdfg", "add_node"].edge_index = torch.cat(
            (data["add_node", "cdfg", "add_node"].edge_index, adjust_edge_index), 1
        )

        adjust_edge_index = adjustEdgeIndex(
            pyg["add_node", "ref", "add_node"].edge_index, addCnt, addCnt
        )
        data["add_node", "ref", "add_node"].edge_index = torch.cat(
            (data["add_node", "ref", "add_node"].edge_index, adjust_edge_index), 1
        )

        delCnt = delCnt + pyg["del_node"].token_ids.size()[0]
        addCnt = addCnt + pyg["add_node"].token_ids.size()[0]

    return data, torch.tensor(del_indexes)


def combinePair(allPairs, max_nodes=256):
    batches = []
    pygs1 = []
    pygs2 = []
    probs = torch.zeros((0, 1), dtype=torch.float)

    nodeCnt = 0
    for pair in allPairs:
        pyg1 = pair["x"].pyg
        pyg2 = pair["y"].pyg
        curCnt = (
            pyg1["add_node"].token_ids.size()[0]
            + pyg1["del_node"].token_ids.size()[0]
            + pyg2["add_node"].token_ids.size()[0]
            + pyg2["del_node"].token_ids.size()[0]
        )

        if nodeCnt + curCnt > max_nodes:
            batchPyg1, delIndex1 = combineGraph(pygs1)
            batchPyg2, delIndex2 = combineGraph(pygs2)

            batch = Batch(batchPyg1, batchPyg2, delIndex1, delIndex2, probs)
            batches.append(batch)

            pygs1 = []
            pygs2 = []
            probs = torch.zeros((0, 1), dtype=torch.float)
            nodeCnt = 0
        else:
            pygs1.append(pyg1)
            pygs2.append(pyg2)
            probs = torch.cat((probs, torch.tensor([[pair["prob"]]])), dim=0)
            nodeCnt = nodeCnt + curCnt

    if len(pygs1) != 0 and len(pygs2) != 0:
        batchPyg1, delIndex1 = combineGraph(pygs1)
        batchPyg2, delIndex2 = combineGraph(pygs2)

        batch = Batch(batchPyg1, batchPyg2, delIndex1, delIndex2, probs)
        batches.append(batch)
    return batches
