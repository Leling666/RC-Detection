seed = 2023
import os

os.environ["PYTHONHASHSEED"] = str(seed)
os.environ["CUBLAS_WORKSPACE_CONFIG"] = ":4096:8"
import random

random.seed(seed)
import numpy as np

np.random.seed(seed)
import torch

torch.manual_seed(seed)
torch.cuda.manual_seed(seed)
torch.cuda.manual_seed_all(seed)
torch.backends.cudnn.deterministic = True
torch.backends.cudnn.benchmark = False
torch.backends.cudnn.enabled = False

from transformers import AutoTokenizer, AutoModel
from torch_geometric.data import HeteroData
from typing import Dict, List, Union

import torch.nn.functional as F
from torch import nn

import torch_geometric.transforms as T
from torch_geometric.nn import GCNConv

from torch_geometric.data import HeteroData
import json
import sys

from itertools import chain
from loss import *
from genPyG import *
from genPairs import *
from genBatch import *
from model import *
from eval import *
from genMiniGraphs import genAllMiniGraphs

def get_all_data():
    with open("miniGraphs.json") as f:
        miniGraphs = json.load(f)
    dataset1 = json.load(open("dataset1.json"))
    dataset2 = json.load(open("dataset2.json"))
    dataset3 = json.load(open("dataset3.json"))
    return (miniGraphs, dataset1, dataset2, dataset3)

def init_model(device, pyg):
    criterion = BCEFocalLoss()
    rgcnModel = RGCN(device, 768, 1, 768 * 2, pyg)
    rgcnModel = rgcnModel.to(device)
    rankNetModel = rankNet(768 * 2)
    rankNetModel = rankNetModel.to(device)
    optimizer = torch.optim.Adam(
        chain(rgcnModel.parameters(), rankNetModel.parameters()), lr=5e-6
    )
    return rgcnModel, rankNetModel, optimizer, criterion

def divide_lst(lst, n, k):
    cnt = 0
    all_list = []
    for i in range(0, len(lst), n):
        if cnt < k - 1:
            all_list.append(lst[i : i + n])
        else:
            all_list.append(lst[i:])
            break
        cnt = cnt + 1
    return all_list

def get_sub_minigraphs(fdirs, all_minigraphs):
    sub_minigraphs = {}
    for fdir in fdirs:
        sub_minigraphs[fdir] = all_minigraphs[fdir]
    return sub_minigraphs

def divide_minigraphs(all_minigraphs, k):
    all_fdirs = []
    for fdir in all_minigraphs.keys():
        all_fdirs.append(fdir)
    all_sub_minigraphs = []
    all_sub_fdirs = []
    for sub_fdirs in divide_lst(all_fdirs, int(len(all_fdirs) / k), k):
        if len(sub_fdirs) == 0:
            continue
        all_sub_fdirs.append(sub_fdirs)
        all_sub_minigraphs.append(get_sub_minigraphs(sub_fdirs, all_minigraphs))
    return all_sub_minigraphs, all_sub_fdirs

def get_all_batchlist(mini_graphs, k, max_pair):
    all_batch_list = []
    pair_cnt = 0
    all_sub_minigraphs, all_sub_fdirs = divide_minigraphs(mini_graphs, k)
    for sub_minigraph in all_sub_minigraphs:
        all_pairs = get_all_pairs(sub_minigraph, max_pair)
        pair_cnt += len(all_pairs)
        batch_list = combinePair(all_pairs, 128)
        all_batch_list.append(batch_list)
    return all_batch_list, all_sub_fdirs, pair_cnt

def train_batchlist(batches, rgcnModel, rankNetModel, optimizer, criterion, device):
    all_loss = []
    rgcnModel.train()
    rankNetModel.train()
    for batch in batches:
        pyg1 = batch.pyg1.clone().to(device)
        pyg2 = batch.pyg2.clone().to(device)
        del_index1 = batch.del_index1.to(device)
        del_index2 = batch.del_index2.to(device)
        probs = batch.probs.to(device)
        x = rgcnModel(pyg1, del_index1)
        y = rgcnModel(pyg2, del_index2)
        optimizer.zero_grad()
        preds = rankNetModel(x, y)
        loss = criterion(preds, probs)
        loss.backward()
        optimizer.step()
        all_loss.append(loss.cpu().detach().item())
    return sum(all_loss)

def validate_batchlist(batches, rgcnModel, rankNetModel, criterion, device):
    all_loss = []
    rgcnModel.eval()
    rankNetModel.eval()
    for batch in batches:
        with torch.no_grad():
            pyg1 = batch.pyg1.clone().to(device)
            pyg2 = batch.pyg2.clone().to(device)
            del_index1 = batch.del_index1.to(device)
            del_index2 = batch.del_index2.to(device)
            probs = batch.probs.to(device)
            x = rgcnModel(pyg1, del_index1)
            y = rgcnModel(pyg2, del_index2)
            preds = rankNetModel(x, y)
            loss = criterion(preds, probs)
            all_loss.append(loss.cpu().detach().item())
    return sum(all_loss)
def do_cross_fold_valid(device, K):
    all_mini_graphs, dataset1, dataset2, dataset3 = get_all_data()#使用 get_all_data 函数获取所有的数据和小图
    all_data = []

    high_ranking_folders = {}

    all_data.extend(dataset1)
    all_data.extend(dataset2)
    all_data.extend(dataset3)
    # print(all_data)
    # print(np.array(all_data).shape)

    random.shuffle(all_data)

    all_data_list = divide_lst(all_data, int(len(all_data) * 0.1), K)#使用 divide_lst 函数将 all_data 分割成 K 个部分，每个部分包含 len(all_data) * 0.1 个元素。
    for i in range(0, len(all_data_list)):
        testset = all_data_list[i]#对于 all_data_list 中的每个部分，将其作为测试集，其余部分作为训练集。
        trainset = []
        for j in range(len(all_data_list)):
            if j != i:
                trainset.extend(all_data_list[j])

        random.shuffle(trainset)

        max_pair = 100
        #从 all_mini_graphs 中提取出 trainset 指定的子图。
        mini_graphs = get_sub_minigraphs(trainset, all_mini_graphs)

        all_batch_list, all_sub_fdirs, pair_cnt = get_all_batchlist(
            mini_graphs, 1, max_pair=max_pair
        )
        #获取一些可能用于后续处理的映射和数据。
        all_true_cid_map = get_true_cid_map(all_data)
        dir_to_minigraphs = get_dir_to_minigraphs(
            get_sub_minigraphs(all_data, all_mini_graphs)
        )
        # print(trainset)
        # print(dir_to_minigraphs)



        rgcnModel, rankNetModel, optimizer, criterion = init_model(
            device, all_batch_list[0][0].pyg1
        )#初始化模型和训练设置。



        epochs = 20

        all_info = []
        for epoch in range(epochs):

            total_train_loss = 0
            total_tp1 = 0
            total_fp1 = 0
            total_tp2 = 0
            total_fp2 = 0
            total_tp3 = 0
            total_fp3 = 0
            total_t = 0

            total_train_loss = total_train_loss + train_batchlist(
                all_batch_list[0], rgcnModel, rankNetModel, optimizer, criterion, device
            )

            eval(trainset, dir_to_minigraphs, rgcnModel, rankNetModel, device)
            tp1, fp1, t = eval_top(
                trainset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                1,
            )
            tp2, fp2, t = eval_top(
                trainset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                2,
            )
            tp3, fp3, t = eval_top(
                trainset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                3,
            )
            total_t = total_t + t
            total_tp1 = total_tp1 + tp1
            total_fp1 = total_fp1 + fp1
            total_tp2 = total_tp2 + tp2
            total_fp2 = total_fp2 + fp2
            total_tp3 = total_tp3 + tp3
            total_fp3 = total_fp3 + fp3
            cur_f1_score = (
                2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
            ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
            info = {}
            info["epoch"] = epoch
            info["pair_cnt"] = pair_cnt
            info["train_loss"] = total_train_loss
            info["train_f1_score"] = (
                2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
            ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
            info["train_top1_f1_precision"] = total_tp1 / (total_tp1 + total_fp1)
            info["train_top1_f1_recall"] = total_tp1 / total_t
            info["train_top2_f1_precision"] = total_tp2 / (total_tp2 + total_fp2)
            info["train_top2_f1_recall"] = total_tp2 / total_t
            info["train_top3_f1_precision"] = total_tp3 / (total_tp3 + total_fp3)
            info["train_top3_f1_recall"] = total_tp3 / total_t
            
            total_tp1 = 0
            total_fp1 = 0
            total_tp2 = 0
            total_fp2 = 0
            total_tp3 = 0
            total_fp3 = 0
            total_t = 0
            eval(testset, dir_to_minigraphs, rgcnModel, rankNetModel, device)
            tp1, fp1, t = eval_top(
                testset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                1,
            )
            tp2, fp2, t = eval_top(
                testset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                2,
            )
            tp3, fp3, t = eval_top(
                testset,
                dir_to_minigraphs,
                rgcnModel,
                rankNetModel,
                device,
                all_true_cid_map,
                3,
            )
            total_t = total_t + t
            total_tp1 = total_tp1 + tp1
            total_fp1 = total_fp1 + fp1
            total_tp2 = total_tp2 + tp2
            total_fp2 = total_fp2 + fp2
            total_tp3 = total_tp3 + tp3
            total_fp3 = total_fp3 + fp3
            cur_f1_score = (
                2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
            ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
            info["test_f1_score"] = (
                2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
            ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
            info["test_top1_f1_precision"] = total_tp1 / (total_tp1 + total_fp1)
            info["test_top1_f1_recall"] = total_tp1 / total_t
            info["test_top2_f1_precision"] = total_tp2 / (total_tp2 + total_fp2)
            info["test_top2_f1_recall"] = total_tp2 / total_t
            info["test_top3_f1_precision"] = total_tp3 / (total_tp3 + total_fp3)
            info["test_top3_f1_recall"] = total_tp3 / total_t
            info["test recall@top1"] = eval_recall_topk(testset, dir_to_minigraphs, 1)
            info["test recall@top2"] = eval_recall_topk(testset, dir_to_minigraphs, 2)
            info["test recall@top3"] = eval_recall_topk(testset, dir_to_minigraphs, 3)
            info["mean_first_rank"] = eval_mean_first_rank(testset, dir_to_minigraphs, high_ranking_folders, epoch)
            all_info.append(info)
            
            print("epoch:",epoch,"train_loss:",info["train_loss"],"recall1:",info["test recall@top1"],"recall2:",info["test recall@top2"],"recall3:",info["test recall@top3"])
  
            if epoch == 19:
                with open(f"./crossfold_result/{i}_{epoch}.json", "w") as f:
                	json.dump(all_info, f)

    #with open("output.json", 'w') as json_file:
                #json.dump(high_ranking_folders, json_file)


def do_cross_project_predict(device):
    all_mini_graphs, dataset1, dataset2, dataset3 = get_all_data()
    
    high_ranking_folders = {}
    
    testset = dataset1
    trainset = []
    trainset.extend(dataset2)
    trainset.extend(dataset3)
    all_data = []
    all_data.extend(dataset1)
    all_data.extend(dataset2)
    all_data.extend(dataset3)

    # print(trainset)
    # print(np.array(trainset).shape)

    max_pair = 100
    mini_graphs = get_sub_minigraphs(trainset, all_mini_graphs)
    all_batch_list, all_sub_fdirs, pair_cnt = get_all_batchlist(
        mini_graphs, 1, max_pair=max_pair
    )
    all_true_cid_map = get_true_cid_map(all_data)
    dir_to_minigraphs = get_dir_to_minigraphs(
        get_sub_minigraphs(all_data, all_mini_graphs)
    )
    # print(trainset)
    # print(dir_to_minigraphs)
    gcnModel, rankNetModel, optimizer, criterion = init_model(
        device, all_batch_list[0][0].pyg1
    )
    epochs = 20
    all_info = []
    for epoch in range(epochs):
        total_train_loss = 0
        total_tp1 = 0
        total_fp1 = 0
        total_tp2 = 0
        total_fp2 = 0
        total_tp3 = 0
        total_fp3 = 0
        total_t = 0
        total_train_loss = total_train_loss + train_batchlist(
            all_batch_list[0], gcnModel, rankNetModel, optimizer, criterion, device
        )
        eval(trainset, dir_to_minigraphs, gcnModel, rankNetModel, device)
        tp1, fp1, t = eval_top(
            trainset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            1,
        )
        tp2, fp2, t = eval_top(
            trainset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            2,
        )
        tp3, fp3, t = eval_top(
            trainset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            3,
        )
        total_t = total_t + t
        total_tp1 = total_tp1 + tp1
        total_fp1 = total_fp1 + fp1
        total_tp2 = total_tp2 + tp2
        total_fp2 = total_fp2 + fp2
        total_tp3 = total_tp3 + tp3
        total_fp3 = total_fp3 + fp3
        cur_f1_score = (
            2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
        ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
        info = {}
        info["epoch"] = epoch
        info["pair_cnt"] = pair_cnt
        info["train_loss"] = total_train_loss
        info["train_f1_score"] = (
            2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
        ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
        info["train_top1_f1_precision"] = total_tp1 / (total_tp1 + total_fp1)
        info["train_top1_f1_recall"] = total_tp1 / total_t
        info["train_top2_f1_precision"] = total_tp2 / (total_tp2 + total_fp2)
        info["train_top2_f1_recall"] = total_tp2 / total_t
        info["train_top3_f1_precision"] = total_tp3 / (total_tp3 + total_fp3)
        info["train_top3_f1_recall"] = total_tp3 / total_t
        
        total_tp1 = 0
        total_fp1 = 0
        total_tp2 = 0
        total_fp2 = 0
        total_tp3 = 0
        total_fp3 = 0
        total_t = 0
        eval(testset, dir_to_minigraphs, gcnModel, rankNetModel, device)
        tp1, fp1, t = eval_top(
            testset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            1,
        )
        tp2, fp2, t = eval_top(
            testset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            2,
        )
        tp3, fp3, t = eval_top(
            testset,
            dir_to_minigraphs,
            gcnModel,
            rankNetModel,
            device,
            all_true_cid_map,
            3,
        )
        total_t = total_t + t
        total_tp1 = total_tp1 + tp1
        total_fp1 = total_fp1 + fp1
        total_tp2 = total_tp2 + tp2
        total_fp2 = total_fp2 + fp2
        total_tp3 = total_tp3 + tp3
        total_fp3 = total_fp3 + fp3
        cur_f1_score = (
            2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
        ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
        info["test_f1_score"] = (
            2 * (total_tp1 / (total_tp1 + total_fp1)) * (total_tp1 / total_t)
        ) / (total_tp1 / (total_tp1 + total_fp1) + total_tp1 / total_t)
        info["test_top1_f1_precision"] = total_tp1 / (total_tp1 + total_fp1)
        info["test_top1_f1_recall"] = total_tp1 / total_t
        info["test_top2_f1_precision"] = total_tp2 / (total_tp2 + total_fp2)
        info["test_top2_f1_recall"] = total_tp2 / total_t
        info["test_top3_f1_precision"] = total_tp3 / (total_tp3 + total_fp3)
        info["test_top3_f1_recall"] = total_tp3 / total_t
        info["test recall@top1"] = eval_recall_topk(testset, dir_to_minigraphs, 1)
        info["test recall@top2"] = eval_recall_topk(testset, dir_to_minigraphs, 2)
        info["test recall@top3"] = eval_recall_topk(testset, dir_to_minigraphs, 3)
        info["mean_first_rank"] = eval_mean_first_rank(testset,dir_to_minigraphs,high_ranking_folders, epoch)
        all_info.append(info)
       
        with open(f"./crossproject_result/{epoch}.json", "w") as f:
            json.dump(all_info, f)


if __name__ == "__main__":
    os.environ['CUDA_VISIBLE_DEVICES'] = '0'
    device = torch.device('cuda' if torch.cuda.is_available()  else 'cpu')
    # device = ''
    genAllMiniGraphs()
    do_cross_fold_valid(device, 10)
    #do_cross_project_predict(device)
