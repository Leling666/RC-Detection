package com.mycompany.parsePatch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.RuntimeErrorException;

/**
 * Patch
 */
public class Patch {
    public class LineInfo {
        public int addLineBeg;
        public int addLineNum;
        public int delLineBeg;
        public int delLineNum;

        public LineInfo(int addLineBeg_, int addLineNum_, int delLineBeg_, int delLineNum_) {
            addLineBeg = addLineBeg_;
            addLineNum = addLineNum_;
            delLineBeg = delLineBeg_;
            delLineNum = delLineNum_;
        }

        @Override
        public String toString() {
            return "(" + addLineBeg + "," + addLineNum + ")(" + delLineBeg + "," + delLineNum + ")";
        }
    }

    String content;

    Pattern fileRegex;
    Pattern lineRegex;
    Pattern endRegex;

    ArrayList<PatchFile> patchFiles;

    String[] lines;

    int lptr;

    String commitId;

    HashMap<String, ArrayList<PatchLine>> addMap;
    HashMap<String, ArrayList<PatchLine>> delMap;
    HashSet<String> specialSet;

    public Patch(String path) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            this.content = content;
            specialSet = new HashSet<>();
            patchFiles = new ArrayList<>();

            addMap = new HashMap<>();
            delMap = new HashMap<>();

            fileRegex = Pattern.compile("^diff(\\s)+--git(\\s)+a/(\\S)+(\\s)+b/(\\S)+");
            lineRegex = Pattern.compile("^@@(\\s)+-([0-9]*),([0-9]*)(\\s)+\\+([0-9]*),([0-9]*)(\\s)+@@");
            endRegex = Pattern.compile("--(\\s)+");
            lines = content.split("\n");
            lptr = 0;
            parse();

            buildMap();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void buildMap() {
        for (var pf : patchFiles) {
            String fullName = pf.fname;

            // assert !addMap.containsKey(sName);
            // assert !delMap.containsKey(sName);

            ArrayList<PatchLine> addPlines = new ArrayList<>();
            ArrayList<PatchLine> delPlines = new ArrayList<>();

            for (var h : pf.getHunk()) {
                for (var pl : h.getLines()) {
                    if (pl.isAdd) {
                        addPlines.add(pl);
                    } else if (pl.isDel) {
                        delPlines.add(pl);
                    }
                }
            }

            addMap.put(fullName, addPlines);
            delMap.put(fullName, delPlines);
        }
    }

    boolean parse() {
        // if (!lines[lptr].startsWith("From")) {
        // return false;
        // }
        // // skip commit id
        // lptr++;
        // // skip author email
        // lptr++;
        // // skip date
        // lptr++;
        // // skip commit msg
        while (!lines[lptr].equals("---")) {
            lptr++;
        }
        lptr++;
        parseFiles();
        return true;
    }

    public LineInfo getLineInfo(String curLine) {

        var m = lineRegex.matcher(curLine);
        if (!m.find()) {
            throw new RuntimeErrorException(null, "can not match line:" + curLine);
        }

        int endPos = m.end();

        curLine = curLine.substring(0, endPos);

        String[] info1 = curLine.split(" ")[1].split(",");
        String[] info2 = curLine.split(" ")[2].split(",");

        int beg1 = Integer.valueOf(info1[0].substring(1));
        int num1 = Integer.valueOf(info1[1]);

        int beg2 = Integer.valueOf(info2[0].substring(1));
        int num2 = Integer.valueOf(info2[1]);

        return new LineInfo(beg2, num2, beg1, num1);
    }

    PatchHunk parseHunk(int ptr1, int ptr2, LineInfo info) {
        int addNum = 0, addLineno = info.addLineBeg, delNum = 0, delLineno = info.delLineBeg;
        PatchHunk h = new PatchHunk();
        for (int i = ptr1; i <= ptr2; ++i) {
            if (lines[i].equals("-- ")) {
                break;
            }
            if (lines[i].startsWith("+")) {
                addNum++;
                h.addLine(new PatchLine(lines[i].substring(1).strip(), false, true, false, addLineno));
                addLineno++;
            } else if (lines[i].startsWith("-")) {
                delNum++;
                h.addLine(new PatchLine(lines[i], true, false, false, delLineno));
                delLineno++;
            } else {
                addLineno++;
                delLineno++;
                addNum++;
                delNum++;
            }
        }
        // if (addNum != info.addLineNum) {
        // System.out.println(lines[ptr2]);
        // String msg = "addNum:" + String.valueOf(addNum) + ",info.addLineNum:" +
        // String.valueOf(info.addLineNum);
        // throw new RuntimeException("line number mismatch:" + msg);
        // }
        // if (delNum != info.delLineNum) {
        // String msg = "delNum:" + String.valueOf(delNum) + ",info.delLineNum:" +
        // String.valueOf(info.delLineNum);
        // throw new RuntimeException("line number mismatch:" + msg);
        // }
        return h;
    }

    PatchFile parseFile(int ptr1, int ptr2) {
        int ptr = ptr1;
        boolean isAdd = false, isDel = false, isMod = false, isRename = false;

        String[] fs = lines[ptr].split(" ");
        String f1 = fs[fs.length - 2].substring(2);
        String f2 = fs[fs.length - 1].substring(2);

        f1 = f1.replaceAll("/", "_");
        f2 = f2.replaceAll("/", "_");
        ptr++;

        if (lines[ptr].startsWith("similarity index") && f1 != f2) {
            specialSet.add(f1);
            specialSet.add(f2);
            isRename = true;
        } else if (lines[ptr].startsWith("deleted file mode")) {
            specialSet.add(f1);
            specialSet.add(f2);
            isDel = true;
        } else if (lines[ptr].startsWith("new file mode")) {
            specialSet.add(f1);
            specialSet.add(f2);
            isAdd = true;
        } else {
            isMod = true;
        }

        while (ptr <= ptr2 && !lineRegex.matcher(lines[ptr]).find()) {
            ptr++;
        }

        PatchFile pf = new PatchFile(f2, isAdd, isDel, isMod, isRename);
        while (ptr <= ptr2 && lineRegex.matcher(lines[ptr]).find()) {
            // System.out.println(lines[ptr]);
            // System.out.println(lineRegex.matcher(lines[ptr]).find());

            LineInfo lineInfo = getLineInfo(lines[ptr]);
            ptr++;
            int beg = ptr;

            while (ptr <= ptr2 && !lineRegex.matcher(lines[ptr]).find()) {
                ptr++;
            }

            pf.addHunk(parseHunk(beg, ptr - 1, lineInfo));
            if (ptr > ptr2) {
                break;
            }
        }

        return pf;
    }

    void parseFiles() {
        while (lptr < lines.length && !fileRegex.matcher(lines[lptr]).find()) {
            lptr++;
        }

        while (lptr < lines.length && fileRegex.matcher(lines[lptr]).find()) {
            int beg = lptr;
            lptr++;

            while (lptr < lines.length && !fileRegex.matcher(lines[lptr]).find()) {
                lptr++;
            }

            this.patchFiles.add(parseFile(beg, lptr - 1));
            if (lptr >= lines.length) {
                break;
            }
        }

    }

    public ArrayList<PatchFile> getPatchFiles() {
        return patchFiles;
    }

    // public ArrayList<PatchLine> getAddLinesByName(String fName) {
    // return addMap.get(fName);
    // }

    // public ArrayList<PatchLine> getDelLinesByName(String fName) {
    // return delMap.get(fName);
    // }

    public boolean isSpecial(String fName) {
        return specialSet.contains(fName);
    }

    public ArrayList<PatchLine> getAddLines(String fName) {
        return addMap.get(fName);
    }

    public ArrayList<PatchLine> getDelLine(String fName) {
        return delMap.get(fName);
    }
}