from parse_patch import patch
import subprocess
import os
import json
import subprocess
import pandas as pd
import traceback
from settings import *
import sys

sys.path.append(os.path.join(SZZ_FOLDER, "tools/pyszz/"))
from szz.ag_szz import AGSZZ
from szz.b_szz import BaseSZZ
from szz.l_szz import LSZZ
from szz.ma_szz import MASZZ, DetectLineMoved
from szz.r_szz import RSZZ
from szz.ra_szz import RASZZ
from szz.pd_szz import PyDrillerSZZ
from szz.my_szz import MySZZ
from szz.core.abstract_szz import AbstractSZZ, ImpactedFile


def gen_joern():
    for i in range(0, 1573):
        fDir = f"../trainData/test{i}"
        beforeDir = os.path.join(fDir, "before")
        afterDir = os.path.join(fDir, "after")

        jBeforeDir = os.path.join(beforeDir, "joern")
        jAfterDir = os.path.join(afterDir, "joern")

        if not os.path.exists(jBeforeDir):
            os.mkdir(jBeforeDir)

        if not os.path.exists(jAfterDir):
            os.mkdir(jAfterDir)
    cmd = "joern  --script genJoern.sc"
    os.system(cmd)


def gen_graph():
    cwd = os.getcwd()
    os.chdir("./my-app")
    os.system("mvn test")
    os.chdir(cwd)


def get_file_content(repo, cid, fname):
    cwd = os.getcwd()
    os.chdir(os.path.join(REPOS_DIR, repo))
    cmd = f"git show {cid}:{fname}"
    content = subprocess.check_output(cmd, shell=True).decode("utf-8", errors="ignore")
    os.chdir(cwd)
    return content


def gen_source_files():
    cnt = 0
    cwd = os.getcwd()
    for i in range(0, 1573):
        fDir = os.path.join("..", "trainData", f"test{i}")
        infoPath = os.path.join(fDir, "info.json")
        with open(infoPath, "r") as f:
            info = json.load(f)

        patchPath = os.path.join(
            fDir, "fixing", os.listdir(os.path.join(fDir, "fixing"))[0]
        )

        with open(patchPath) as f:
            p = patch(f.read())
        files = []
        for f in p.get_files():
            if f.is_add or f.is_del or f.is_rename:
                continue
            files.append(f.file_name)
        try:
            for file in files:
                simple_file_name = file.replace("/", "_")
                simple_file_path1 = os.path.join(fDir, "after", simple_file_name)
                with open(simple_file_path1, "w") as f:
                    f.write(get_file_content(info["repo"], info["fix"], file))

                simple_file_path2 = os.path.join(fDir, "before", simple_file_name)
                with open(simple_file_path2, "w") as f:
                    f.write(get_file_content(info["repo"], f"{info['fix']}^1", file))
            cnt = cnt + 1
        except Exception as e:
            os.chdir(cwd)
            with open("log.txt", "a") as f:
                f.write(traceback.format_exc())
                json.dump(info, f)
                f.write(fDir)
                f.write("\n")


def run_szz(
    curSZZ, project, commit, repo_url=None, repos_dir=REPOS_DIR, use_temp_dir=False
):
    b_szz = curSZZ(
        repo_full_name=project,
        repo_url=repo_url,
        repos_dir=REPOS_DIR,
        use_temp_dir=use_temp_dir,
    )

    imp_files = b_szz.get_impacted_files(
        fix_commit_hash=commit, file_ext_to_parse=["java"], only_deleted_lines=True
    )

    bug_introducing_commits = b_szz.find_bic(
        fix_commit_hash=commit, impacted_files=imp_files, ignore_revs_file_path=None
    )

    return [commit.hexsha for commit in bug_introducing_commits]


def run_szz_fdirs():
    for i in range(0, 1488):
        fdir = f"test{i}"
        info = json.load(open(os.path.join(DATA_PATH, fdir, "info.json")))
        szz_result = {}
        fix_cid = info["fix"]
        project = info["repo"]
        repo_dir = os.path.join(REPOS_DIR, project)
        all_szz = [BaseSZZ, AGSZZ, MASZZ, RASZZ]

        for szz in all_szz:
            induce_cids = run_szz(szz, project, fix_cid)
            szz_result.append({"szz_type": szz.__name__, "commits": induce_cids})

        with open(os.path.join(DATA_PATH, fdir, "szz_result.json"), "w") as f:
            json.dump(szz_result, f)


def run_szz_nodes():
    for i in range(0, 1488):
        fDir = f"test{i}"

        infoPath = f"{fDir}/info.json"
        info = None
        with open(infoPath, "r") as f:
            info = json.load(f)

        graphPath = f"{fDir}/graph.json"
        graph = None
        with open(graphPath, "r") as f:
            graph = json.load(f)

        bszz = BaseSZZ(info["repo"], None, REPOS_DIR, False)
        commits = []

        f = False
        finalNodes = []
        curAddNode = 0
        curDelNode = 0
        for node in graph:
            modLines = []
            fileName = node["fName"].replace("_", "/")
            begLine = node["lineBeg"]
            endLine = node["lineEnd"]

            for i in range(begLine, endLine + 1):
                modLines.append(i)

            impactFile = ImpactedFile(fileName, modLines)

            node["rootcause"] = False
            node["commits"] = []
            if node["isDel"]:
                bug_cids = [
                    commit.hexsha for commit in bszz.find_bic(info["fix"], [impactFile])
                ]
                node["commits"] = bug_cids
                for cid in bug_cids:
                    for cid1 in info["induce"]:
                        if cid in cid1 or cid1 in cid:
                            f = True
                            node["rootcause"] = True
                commits.extend(bug_cids)
                curDelNode = curDelNode + 1
            else:
                curAddNode = curAddNode + 1

            finalNodes.append(node)

        with open(f"{fDir}/graph1.json", "w") as f:
            json.dump(finalNodes, f)


def run_szz_nodes1():
    for i in range(1488, 1573):
        fDir = f"test{i}"

        infoPath = f"{fDir}/info.json"
        info = None
        with open(infoPath, "r") as f:
            info = json.load(f)

        graphPath = f"{fDir}/graph.json"
        graph = None
        with open(graphPath, "r") as f:
            graph = json.load(f)

        bszz = MySZZ(info["repo"], None, REPOS_DIR, False)
        commits = []

        f = False
        finalNodes = []
        curAddNode = 0
        curDelNode = 0
        for node in graph:
            modLines = []
            fileName = node["fName"].replace("_", "/")
            begLine = node["lineBeg"]
            endLine = node["lineEnd"]

            for i in range(begLine, endLine + 1):
                modLines.append(i)

            impactFile = ImpactedFile(fileName, modLines)

            node["rootcause"] = False
            node["commits"] = []
            if node["isDel"]:
                bug_cids = [
                    commit.hexsha for commit in bszz.find_bic(info["fix"], [impactFile])
                ]
                node["commits"] = bug_cids
                for cid in bug_cids:
                    for cid1 in info["induce"]:
                        if cid in cid1 or cid1 in cid:
                            f = True
                            node["rootcause"] = True
                commits.extend(bug_cids)
                curDelNode = curDelNode + 1
            else:
                curAddNode = curAddNode + 1

            finalNodes.append(node)

        with open(f"{fDir}/graph1.json", "w") as f:
            json.dump(finalNodes, f)


if __name__ == "__main__":
    gen_source_files()
    run_szz_fdirs()
    gen_joern()
    gen_graph()
    run_szz_nodes()
    run_szz_nodes1()
