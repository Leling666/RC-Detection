from torch.utils.data import Dataset, DataLoader
import torch
from transformers import AutoTokenizer, AutoModel
from torch_geometric.data import HeteroData
from typing import Dict, List, Union
import copy
import torch
import torch.nn.functional as F
from torch import nn

import torch_geometric.transforms as T
from torch_geometric.nn import RGCNConv

from torch_geometric.data import HeteroData
import json
import random


class BCEFocalLoss(torch.nn.Module):
    def __init__(self, gamma=3, alpha=0.25, reduction='mean'):
        super(BCEFocalLoss, self).__init__()
        self.gamma = gamma
        self.alpha = alpha
        self.reduction = reduction

    def forward(self, predict, target):
        pt = predict
        loss = - ((1 - self.alpha) * ((1 - pt + 1e-5) ** self.gamma) * (target * torch.log(pt + 1e-5)) + self.alpha * (
                (pt + +1e-5) ** self.gamma) * ((1 - target) * torch.log(1 - pt + 1e-5)))

        # 计算需要保留的损失值数量（95%）
        num_to_keep = int(0.95 * loss.size(0))

        # 对损失值进行排序
        sorted_loss, _ = torch.sort(loss, descending=False)

        # 仅保留前 95% 的损失值
        loss_to_average = sorted_loss[:num_to_keep]

        if self.reduction == 'mean':
            loss = torch.mean(loss_to_average)
        elif self.reduction == 'sum':
            loss = torch.sum(loss)
        return loss


class BCE_WITH_WEIGHT(torch.nn.Module):
    def __init__(self, alpha=0.25, reduction='mean'):
        super(BCE_WITH_WEIGHT, self).__init__()
        self.alpha = alpha
        self.reduction = reduction

    def forward(self, predict, target):
        pt = predict
        loss = -((1 - self.alpha) * target * torch.log(pt + 1e-5) + self.alpha * (1 - target) * torch.log(
            1 - pt + 1e-5))
        if self.reduction == 'mean':
            loss = torch.mean(loss)
        elif self.reduction == 'sum':
            loss = torch.sum(loss)

        return loss


def _expand_binary_labels(labels, label_weights, label_channels):
    bin_labels = labels.new_full((labels.size(0), label_channels), 0)
    inds = torch.nonzero(labels >= 1).squeeze()
    if inds.numel() > 0:
        bin_labels[inds, labels[inds]] = 1
    bin_label_weights = label_weights.view(-1, 1).expand(label_weights.size(0), label_channels)
    return bin_labels, bin_label_weights


class GHMC(nn.Module):
    """GHM Classification Loss.
    Ref:https://github.com/libuyu/mmdetection/blob/master/mmdet/models/losses/ghm_loss.py
    Details of the theorem can be viewed in the paper
    "Gradient Harmonized Single-stage Detector".
    https://arxiv.org/abs/1811.05181

    Args:
        bins (int): Number of the unit regions for distribution calculation.
        momentum (float): The parameter for moving average.
        use_sigmoid (bool): Can only be true for BCE based loss now.
        loss_weight (float): The weight of the total GHM-C loss.
    """

    def __init__(self, bins=10, momentum=0, use_sigmoid=True, loss_weight=1.0, alpha=None):
        super(GHMC, self).__init__()
        self.bins = bins
        self.momentum = momentum
        edges = torch.arange(bins + 1).float() / bins
        self.register_buffer('edges', edges)
        self.edges[-1] += 1e-6
        if momentum > 0:
            acc_sum = torch.zeros(bins)
            self.register_buffer('acc_sum', acc_sum)
        self.use_sigmoid = use_sigmoid
        if not self.use_sigmoid:
            raise NotImplementedError
        self.loss_weight = loss_weight

        self.label_weight = alpha

    def forward(self, pred, target, label_weight=None, *args, **kwargs):
        """Calculate the GHM-C loss.

        Args:
            pred (float tensor of size [batch_num, class_num]):
                The direct prediction of classification fc layer.
            target (float tensor of size [batch_num, class_num]):
                Binary class target for each sample.
            label_weight (float tensor of size [batch_num, class_num]):
                the value is 1 if the sample is valid and 0 if ignored.
        Returns:
            The gradient harmonized loss.
        """
        # the target should be binary class label

        # if pred.dim() != target.dim():
        #     target, label_weight = _expand_binary_labels(
        #     target, label_weight, pred.size(-1))

        # 我的pred输入为[B,C]，target输入为[B]
        target = torch.zeros(target.size(0), 2).to(target.device).scatter_(1, target.view(-1, 1), 1)

        # 暂时不清楚这个label_weight输入形式，默认都为1
        if label_weight is None:
            label_weight = torch.ones([pred.size(0), pred.size(-1)]).to(target.device)

        target, label_weight = target.float(), label_weight.float()
        edges = self.edges
        mmt = self.momentum
        weights = torch.zeros_like(pred)

        # gradient length
        # sigmoid梯度计算
        g = torch.abs(pred.sigmoid().detach() - target)
        # 有效的label的位置
        valid = label_weight > 0
        # 有效的label的数量
        tot = max(valid.float().sum().item(), 1.0)
        n = 0  # n valid bins
        for i in range(self.bins):
            # 将对应的梯度值划分到对应的bin中， 0-1
            inds = (g >= edges[i]) & (g < edges[i + 1]) & valid
            # 该bin中存在多少个样本
            num_in_bin = inds.sum().item()
            if num_in_bin > 0:
                if mmt > 0:
                    # moment计算num bin
                    self.acc_sum[i] = mmt * self.acc_sum[i] \
                                      + (1 - mmt) * num_in_bin
                    # 权重等于总数/num bin
                    weights[inds] = tot / self.acc_sum[i]
                else:
                    weights[inds] = tot / num_in_bin
                n += 1
        if n > 0:
            # scale系数
            weights = weights / n

        loss = F.binary_cross_entropy_with_logits(
            pred, target, weights, reduction='sum') / tot
        return loss * self.loss_weight

