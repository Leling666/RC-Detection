import os
import json
import subprocess
import pandas as pd
import traceback
from settings import *
import os
import pandas as pd
import os
import json
import subprocess
import pandas as pd
import traceback


def get_patch_content(repo, cid):
    cwd = os.getcwd()
    os.chdir(os.path.join(REPO_DIR, repo))
    cmd = f"git format-patch -1 --stdout {cid}"
    patch = subprocess.check_output(cmd, shell=True).decode("utf-8", errors="ignore")
    os.chdir(cwd)
    return patch


def gen_data1():
    cnt = 0
    file_list = ["ACCUMULO.csv", "AMBARI.csv", "HADOOP.csv", "LUCENE.csv", "OOZIE.csv"]
    for file in file_list:
        df = pd.read_csv(os.path.join("../rawData", file), sep="\t")
        repo = file[: file.find(".")].lower()
        for line in df.values:
            try:
                info = {}
                info["repo"] = repo
                info["fix"] = line[1].split(",")
                info["induce"] = line[2].split(",")
                for fix_id in info["fix"]:
                    info1 = {}
                    info1["repo"] = repo
                    info1["fix"] = fix_id
                    info1["induce"] = line[2].split(",")
                    patchContent = get_patch_content(repo, fix_id)
                    patchContents = []
                    for inducing_id in info["induce"]:
                        patchContents.append(get_patch_content(repo, inducing_id))

                    dirName = f"test{cnt}"
                    cnt = cnt + 1
                    basePath = os.path.join("../trainData", dirName)
                    os.mkdir(basePath)
                    with open(os.path.join(basePath, "info.json"), "w") as f:
                        json.dump(info1, f)
                    dirBefore = os.path.join(basePath, "before")
                    dirAfter = os.path.join(basePath, "after")
                    os.mkdir(dirBefore)
                    os.mkdir(dirAfter)

                    inducing_path = os.path.join(basePath, "inducing")
                    fixing_path = os.path.join(basePath, "fixing")
                    os.mkdir(inducing_path)
                    os.mkdir(fixing_path)
                    with open(os.path.join(fixing_path, fix_id), "w") as f:
                        f.write(patchContent)
                    for patchContent in patchContents:
                        with open(os.path.join(inducing_path, inducing_id), "w") as f:
                            f.write(patchContent)
            except Exception as e:
                with open("log.txt", "a") as f:
                    f.write(traceback.format_exc())
                    f.write("\n")


def gen_data2():
    beg_cnt = 242
    df = pd.read_excel(os.path.join("../rawData", "regressions_in_paper.xlsx"))
    for line in df.values:
        info = {}
        info["repo"] = line[0]
        info["fix"] = line[1]
        info["induce"] = []
        info["induce"].append(line[2])
        try:
            patch1 = get_patch_content(info["repo"], info["fix"])
            patch2 = get_patch_content(info["repo"], info["induce"][0])

            dirName = f"test{cnt}"
            basePath = os.path.join("../trainData", dirName)
            os.mkdir(basePath)
            with open(os.path.join(basePath, "info.json"), "w") as f:
                json.dump(info, f)
            dirBefore = os.path.join(basePath, "before")
            dirAfter = os.path.join(basePath, "after")
            os.mkdir(dirBefore)
            os.mkdir(dirAfter)

            inducing_path = os.path.join(basePath, "inducing")
            fixing_path = os.path.join(basePath, "fixing")

            os.mkdir(inducing_path)
            os.mkdir(fixing_path)

            with open(os.path.join(fixing_path, info["fix"]), "w") as f:
                f.write(patch1)

            with open(os.path.join(inducing_path, info["induce"][0]), "w") as f:
                f.write(patch2)
            cnt = cnt + 1
        except Exception as e:
            with open("log1.txt", "a") as f:
                f.write(traceback.format_exc())
                f.write("\n")


def gain_map():
    cwd = os.getcwd()
    os.chdir(os.path.join(REPO_DIR, "commons-lang"))

    output = subprocess.check_output("git log --oneline --all", shell=True).decode(
        "utf-8"
    )

    lines = output.split("\n")

    commits = []

    for line in lines:
        if line != "":
            commits.append(line.split(" ")[0])

    smap = {}

    for commit in commits:
        gitLog = subprocess.check_output(f"git log {commit} -1", shell=True).decode(
            "utf-8"
        )
        svnId = ""
        lines = gitLog.split("\n")
        for line in lines:
            line = line.strip()
            if "git-svn-id:" in line:
                svnId = line.split(" ")[1].split("@")[1]
        if svnId != "":
            smap[svnId] = commit

    os.chdir(cwd)
    with open("commons-lang.json", "w") as f:
        json.dump(smap, f)

    os.chdir(os.path.join(cwd, "commons-math"))

    output = subprocess.check_output("git log --oneline --all", shell=True).decode(
        "utf-8"
    )

    lines = output.split("\n")

    commits = []

    for line in lines:
        if line != "":
            commits.append(line.split(" ")[0])

    smap = {}

    for commit in commits:
        gitLog = subprocess.check_output(f"git log {commit} -1", shell=True).decode(
            "utf-8"
        )
        svnId = ""
        lines = gitLog.split("\n")
        for line in lines:
            line = line.strip()
            if "git-svn-id:" in line:
                svnId = line.split(" ")[1].split("@")[1]
        if svnId != "":
            smap[svnId] = commit

    os.chdir(cwd)
    with open("commons-math.json", "w") as f:
        json.dump(smap, f)


def gen_data3():
    gain_map()
    df = pd.read_csv("../rawData/dataset_bugfix_bic.csv")

    infos = []
    curInfo = None

    preBugfix = ""

    with open("commons-lang.json") as f:
        map1 = json.load(f)

    with open("commons-math.json") as f:
        map2 = json.load(f)

    tset = set()

    for line in df.values:
        repo = line[0].split(";")[1]
        bugFix = line[0].split(";")[3]
        bugInduce = line[0].split(";")[6]

        tset.add(line[0].split(";")[0] + repo)

        if repo == "commons-lang":
            if bugFix not in map1 or bugInduce not in map1:
                continue
            else:
                bugFix = map1[bugFix]
                bugInduce = map1[bugInduce]

        if repo == "commons-math":
            if bugFix not in map2 or bugInduce not in map2:
                continue
            else:
                bugFix = map2[bugFix]
                bugInduce = map2[bugInduce]

        if bugFix != preBugfix:
            if curInfo != None:
                infos.append(curInfo)
            curInfo = {}
            curInfo["repo"] = repo
            curInfo["fix"] = bugFix
            curInfo["induce"] = []
            curInfo["induce"].append(bugInduce)
            preBugfix = bugFix
        else:
            curInfo["induce"].append(bugInduce)

    cnt = 1198

    for info in infos:
        repo = info["repo"]
        fix_id = info["fix"]
        try:
            patchContent = get_patch_content(repo, fix_id)
            patchContents = []
            for inducing_id in info["induce"]:
                patchContents.append(get_patch_content(repo, inducing_id))
            dirName = f"test{cnt}"

            basePath = os.path.join("../trainData", dirName)
            os.mkdir(basePath)
            with open(os.path.join(basePath, "info.json"), "w") as f:
                json.dump(info, f)
            dirBefore = os.path.join(basePath, "before")
            dirAfter = os.path.join(basePath, "after")
            os.mkdir(dirBefore)
            os.mkdir(dirAfter)

            inducing_path = os.path.join(basePath, "inducing")
            fixing_path = os.path.join(basePath, "fixing")
            os.mkdir(inducing_path)
            os.mkdir(fixing_path)
            with open(os.path.join(fixing_path, fix_id), "w") as f:
                f.write(patchContent)
            for patchContent in patchContents:
                with open(os.path.join(inducing_path, inducing_id), "w") as f:
                    f.write(patchContent)
            cnt = cnt + 1
        except Exception as e:
            with open("log2.txt", "a") as f:
                f.write(traceback.format_exc())
                f.write("\n")


def gen_data4():
    RAW_DATA_PATH1 = os.path.join(
        "../rawData",
        "label.json",
    )
    RAW_DATA_PATH2 = os.path.join(
        "../rawData",
        "verified_cve_with_versions_Java.json",
    )

    with open(RAW_DATA_PATH1) as f:
        label_data = json.load(f)

    with open(RAW_DATA_PATH2) as f:
        verified_cves = json.load(f)

    infos = []
    for cve in verified_cves:
        repo = cve["project"]
        cve_id = cve["cve_id"]
        for fixing_detail in cve["fixing_details"]:
            fix_cid = fixing_detail["fixing_commit"]
            induce_cid = ""
            for inducing_commit_info in fixing_detail["inducing_commits"]:
                if inducing_commit_info["is_true_inducing"] == "True":
                    induce_cid = inducing_commit_info["commit_id"]
            if induce_cid == "":
                continue

            cur_info = {}
            cur_info["repo"] = repo
            cur_info["fix"] = fix_cid
            cur_info["induce"] = []
            cur_info["induce"].append(induce_cid)
            infos.append(cur_info)

    # %%
    cnt = 1488

    for info in infos:
        repo = info["repo"]
        fix_id = info["fix"]
        try:
            patchContent = get_patch_content(repo, fix_id)
            patchContents = []
            for inducing_id in info["induce"]:
                patchContents.append(get_patch_content(repo, inducing_id))
            dirName = f"test{cnt}"

            basePath = os.path.join("../trainData", dirName)
            os.mkdir(basePath)
            with open(os.path.join(basePath, "info.json"), "w") as f:
                json.dump(info, f)
            dirBefore = os.path.join(basePath, "before")
            dirAfter = os.path.join(basePath, "after")
            os.mkdir(dirBefore)
            os.mkdir(dirAfter)

            inducing_path = os.path.join(basePath, "inducing")
            fixing_path = os.path.join(basePath, "fixing")
            os.mkdir(inducing_path)
            os.mkdir(fixing_path)
            with open(os.path.join(fixing_path, fix_id), "w") as f:
                f.write(patchContent)
            for patchContent in patchContents:
                with open(os.path.join(inducing_path, inducing_id), "w") as f:
                    f.write(patchContent)
            cnt = cnt + 1
        except Exception as e:
            with open("log3.txt", "a") as f:
                f.write(traceback.format_exc())
                f.write("\n")


if __name__ == "__main__":
    gen_data1()
    gen_data2()
    gen_data3()
    gen_data4()
