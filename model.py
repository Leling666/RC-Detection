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

class RGCN(nn.Module):
    def __init__(self, device, in_channels, hidden_channels, out_channels, pyg):
        super(RGCN, self).__init__()
        self.device = device
        self.bert_model = AutoModel.from_pretrained("microsoft/codebert-base")
        self.node_types, self.edge_types = pyg.metadata()
        num_relations = len(self.edge_types)
        self.conv1 = RGCNConv(in_channels, hidden_channels, num_relations=num_relations, num_bases=30)
        self.conv2 = RGCNConv(hidden_channels, out_channels, num_relations=num_relations, num_bases=30)

    def forward(self, pyg, delIndexes):
        token_ids_dict = pyg.token_ids_dict
        if token_ids_dict["add_node"].numel() != 0:
            pyg["add_node"].x = self.bert_model(
                torch.tensor(
                    token_ids_dict["add_node"].tolist(),
                    dtype=torch.long,
                    device=self.device,
                )
            )[0][:, 0, :]
        if token_ids_dict["del_node"].numel() != 0:
            pyg["del_node"].x = self.bert_model(
                torch.tensor(
                    token_ids_dict["del_node"].tolist(),
                    dtype=torch.long,
                    device=self.device,
                )
            )[0][:, 0, :]
        if token_ids_dict["add_node"].numel() == 0:
            pyg["add_node"].x = torch.zeros(
                (0, 768), dtype=torch.float, device=self.device
            )
        if token_ids_dict["del_node"].numel() == 0:
            pyg["del_node"].x = torch.zeros(
                (0, 768), dtype=torch.float, device=self.device
            )

        addnum_nodes = pyg['add_node'].num_nodes
        delnum_nodes = pyg['del_node'].num_nodes
        data = pyg
        homogeneous_data = data.to_homogeneous()
        edge_index, edge_type = homogeneous_data.edge_index, homogeneous_data.edge_type
        x = self.conv1(homogeneous_data.x, edge_index, edge_type)
        x = self.conv2(x, edge_index, edge_type)
        x = x[-delnum_nodes:]
        return torch.index_select(x, 0, delIndexes)

    def predict(self, pyg, delIndexes):
        token_ids_dict = pyg.token_ids_dict
        if token_ids_dict["add_node"].numel() != 0:
            pyg["add_node"].x = self.bert_model(
                torch.tensor(
                    token_ids_dict["add_node"].tolist(),
                    dtype=torch.long,
                    device=self.device,
                )
            )[0][:, 0, :]
        if token_ids_dict["del_node"].numel() != 0:
            pyg["del_node"].x = self.bert_model(
                torch.tensor(
                    token_ids_dict["del_node"].tolist(),
                    dtype=torch.long,
                    device=self.device,
                )
            )[0][:, 0, :]
        if token_ids_dict["add_node"].numel() == 0:
            pyg["add_node"].x = torch.zeros(
                (0, 768), dtype=torch.float, device=self.device
            )
        if token_ids_dict["del_node"].numel() == 0:
            pyg["del_node"].x = torch.zeros(
                (0, 768), dtype=torch.float, device=self.device
            )

        addnum_nodes = pyg['add_node'].num_nodes
        delnum_nodes = pyg['del_node'].num_nodes
        data = pyg
        homogeneous_data = data.to_homogeneous()
        edge_index, edge_type = homogeneous_data.edge_index, homogeneous_data.edge_type
        x = self.conv1(homogeneous_data.x, edge_index, edge_type)
        x = self.conv2(x, edge_index, edge_type)
        x = x[-delnum_nodes:]
        return torch.index_select(x, 0, delIndexes)

class rankNet(nn.Module):
    def __init__(self, num_features):
        super().__init__()
        self.model = nn.Sequential(
            nn.Linear(num_features, 32),
            nn.Linear(32, 16),
            nn.Linear(16, 8),
            nn.Linear(8, 1),
        )
        self.output = nn.Sigmoid()

    def forward(self, input1, input2):
        s1 = self.model(input1)
        s2 = self.model(input2)
        return self.output(s1 - s2)

    def predict(self, input):
        return self.model(input)
