# RC-Detection
Identifying Root Cause of bugs by Capturing Changed Code with Relational Graph Neural Networks

## Dataset:
The datasets are collected from 87 open-source projects, including 675 bug-fix commits.
Each directory in trainData contains three files.
- `info.json`: contains the repository's name, the bug-fixing commit and the bug-inducing commit. 
- `graph1.json`: contains the heterogeneous graph generated for the bug-fixing commit in json format. 
- `graph2.json`: contain our manual annotation result.

The files `dataset1.json`,`dataset2.json` and `dataset3.json` contain the filtered datasets for the DATASET1, DATASET2 and DATASET3 used in our experiment respectively.
## Main File
- eval.py: some functions used to evaluate the effectiveness of our model
- genPyG.py: convert the graph in the trainData directory to PYG format
- genMiniGraphs.py: group all heterogeneous graphs in trainData into a single file for training
- genPairs.py: generate pairs for the ranknet model
- genBatch.py: combine multiple pairs into a batch
- model.py: the defined model
- train.py: train the model and get the result
- loss.py: the loss funtions used in our model
- rawData: the original dataset, we download all original data from other papers here 
- processRawData: the code to process the raw data
- buildGraph: the code to build the heterogeneous graph
- trainData: the generated heterogeneous graph for each bug-fixing com
## Experimental environment：
1. OS: Ubuntu
  
2. GPU: NVIDIA RTX 3090.
   
3. Language: Python (v3.7)

4. CUDA: 12.4

5. other packages please refer to requirements.md

## Train
-'run':For running the cross fold validation , run `train.py`.
-'output': the results of the cross fold validation are in the 'crossfold_result'




