# %%

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
from itertools import chain
from functools import cmp_to_key


def get_true_cid_map(fdirs):
    true_cid_map = {}
    for fdir in fdirs:
        true_cid_map[fdir] = set()
        with open(f"trainData/{fdir}/info.json") as f:
            info = json.load(f)
            for cid in set(info["induce"]):
                true_cid_map[fdir].add(cid)
    return true_cid_map



def get_true_cid_set(fdirs):
    true_cid_set = set()
    for fdir in fdirs:
        with open(f"data/{fdir}/info.json") as f:
            info = json.load(f)
            for cid in info["induce"]:
                true_cid_set.add(cid)

    return true_cid_set



def get_score(pyg, gcnModel, rankNetModel, device):
    with torch.no_grad():
        gcnModel.eval()
        rankNetModel.eval()
        pyg = pyg.to(device)
        h = gcnModel.predict(pyg, torch.tensor([0], device=device))[0]
        return rankNetModel.predict(h)


# %%
def cmp(x, y):
    score1 = x.score.double()
    score2 = y.score.double()
    if score1 < score2:
        return 1
    elif score1 > score2:
        return -1
    return 0


# %%
def eval(fdirs, dir_to_minigraphs, gcnModel, rankNetModel, device):
    gcnModel.eval()
    rankNetModel.eval()
    with torch.no_grad():
        all_error_info = []
        for fdir in fdirs:
            for mini_graph in dir_to_minigraphs[fdir]:
                pyg = mini_graph.pyg
                score = get_score(pyg, gcnModel, rankNetModel, device).to("cpu")
                mini_graph.score = score
            dir_to_minigraphs[fdir].sort(key=cmp_to_key(cmp))
            if (
                len(dir_to_minigraphs[fdir]) > 0
                and len(dir_to_minigraphs[fdir][0].g) > 0
                and dir_to_minigraphs[fdir][0].g[0]["rootcause"] == False
            ):
                error_info = {}
                error_info["dir"] = fdir
                error_info["code"] = dir_to_minigraphs[fdir][0].g[0]["code"]
                all_error_info.append(error_info)


# %%
# return tp,fp
def eval_top(fdirs, dir_to_minigraphs, gcnModel, rankNetModel, device, true_cid_map, k):
    gcnModel.eval()
    rankNetModel.eval()
    with torch.no_grad():
        # eval(fdirs, dir_to_minigraphs, gcnModel, rankNetModel, device)
        tp = 0
        fp = 0
        total_t = 0
        for fdir in fdirs:
            cidSet = set()
            total_t = total_t + len(true_cid_map[fdir])
            for mini_graph in dir_to_minigraphs[fdir][:k]:
                node = mini_graph.g[0]

                f = False
                for cid in node["commits"]:
                    if cid not in cidSet and cid in true_cid_map[fdir]:
                        f = True
                        cidSet.add(cid)
                # the node is rootcause
                if f:
                    tp = tp + 1
                    continue

                # each node should correspond to one commit
                f1 = False
                for cid in node["commits"]:
                    if cid not in cidSet:
                        cidSet.add(cid)
                        f1 = True
                if f1:
                    fp = fp + 1

        return tp, fp, total_t


def eval_recall_topk(fdirs, dir_to_minigraphs, k):
    root_cause_cnt = 0
    for fdir in set(fdirs):
        f = False
        for mini_graph in dir_to_minigraphs[fdir][:k]:
            node = mini_graph.g[0]
            if node["rootcause"]:
                f = True
        if f == True:
            root_cause_cnt = root_cause_cnt + 1
    return root_cause_cnt / len(set(fdirs))




def eval_mean_first_rank(fdirs, dir_to_minigraphs, high_ranking_folders, epoch):
    total_rank_cnt = 0
    for fdir in set(fdirs):
        for i, mini_graph in enumerate(dir_to_minigraphs[fdir]):
            node = mini_graph.g[0]
            if node["rootcause"]:
                total_rank_cnt = total_rank_cnt + i + 1
                if epoch == 19 and i + 1 > 5:
                    high_ranking_folders[fdir] = i + 1
                break
    return total_rank_cnt / len(set(fdirs))
