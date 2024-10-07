
import torch
from torch.utils.data import Dataset, DataLoader
from transformers import AutoTokenizer, AutoModel
from torch_geometric.data import HeteroData
from typing import Dict, List, Union

import torch
import torch.nn.functional as F
from torch import nn

import torch_geometric.transforms as T
from torch_geometric.nn import HANConv

from torch_geometric.data import HeteroData
import json
import random
import sys

from itertools import chain
import os
from genPyG import *
from genPairs import *
from genBatch import *
from model import *
from eval import *


from train import *


def load_model(device, han_model_path, ranknet_model_path, metadata):
    hanModel = HAN(device, 768, 768*2, metadata=metadata,heads=2)
    hanModel.load_state_dict(torch.load(han_model_path, map_location=device))
    hanModel = hanModel.to(device)
    rankNetModel = rankNet(768*2)
    rankNetModel.load_state_dict(torch.load(ranknet_model_path, map_location=device))
    rankNetModel = rankNetModel.to(device)

    hanModel.eval()
    rankNetModel.eval()
    return hanModel, rankNetModel

# %%
def eval_dataset(dataset, device, han_model_path, ranknet_model_path, all_mini_graphs):
    mini_graphs = get_sub_minigraphs(dataset, all_mini_graphs)
    K = 1
    all_batch_list, all_sub_fdirs, pair_cnt = get_all_batchlist(mini_graphs, k=K, max_pair = 100)
    all_true_cid_map = get_true_cid_map(dataset)
    dir_to_minigraphs = get_dir_to_minigraphs(mini_graphs)
    han_model, ranknet_model = load_model(
        device, han_model_path, ranknet_model_path, all_batch_list[0][0].pyg1.metadata()
    )
    eval(all_sub_fdirs[0], dir_to_minigraphs, han_model, ranknet_model, device)
    tp1, fp1, total_t1 = eval_top(
        all_sub_fdirs[0],
        dir_to_minigraphs,
        han_model,
        ranknet_model,
        device,
        all_true_cid_map,
        1,
    )
    tp2, fp2, total_t2 = eval_top(
        all_sub_fdirs[0],
        dir_to_minigraphs,
        han_model,
        ranknet_model,
        device,
        all_true_cid_map,
        2,
    )
    tp3, fp3, total_t3 = eval_top(
        all_sub_fdirs[0],
        dir_to_minigraphs,
        han_model,
        ranknet_model,
        device,
        all_true_cid_map,
        3,
    )
    return (
        tp1,
        fp1,
        total_t1,
        tp2,
        fp2,
        total_t2,
        tp3,
        fp3,
        total_t3,
        dir_to_minigraphs
    )


all_mini_graphs, trainset, testset = get_all_data()


device = ""
han_model_path = "./train1model/hanModel.cpkt"
ranknet_model_path = "./train1model/rankNetModel.cpkt"
(
    tp1,
    fp1,
    total_t1,
    tp2,
    fp2,
    total_t2,
    tp3,
    fp3,
    total_t3,
    dir_to_minigraphs
) = eval_dataset(
    testset, device, han_model_path, ranknet_model_path, all_mini_graphs
)



print(f"evaluate result for dataset1 commits:")
print(f"top@1 precision{tp1/(fp1+tp1)},recall{tp1/total_t1}")
print(f"top@2 precision{tp2/(fp2+tp2)},recall{tp2/total_t2}")
print(f"top@3 precision{tp3/(fp3+tp3)},recall{tp3/total_t3}")
print(f'recall at top1 {eval_recall_topk(testset,dir_to_minigraphs,1)}')
print(f'recall at top2 {eval_recall_topk(testset,dir_to_minigraphs,2)}')
print(f'recall at top3 {eval_recall_topk(testset,dir_to_minigraphs,3)}')
print(f'mean first rank {eval_mean_first_rank(testset,dir_to_minigraphs)}')
print(f'mean average rank {eval_mean_average_rank(testset,dir_to_minigraphs)}')



