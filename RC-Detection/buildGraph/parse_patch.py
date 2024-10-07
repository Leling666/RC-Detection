import re
from time import strftime, strptime


class line:
    def __init__(self, line, is_add, is_del, is_bg):
        self.line = line
        self.is_add = is_add
        self.is_del = is_del
        self.is_bg = is_bg

    def __str__(self) -> str:
        return str(
            {
                "line": self.line,
                "is_add": self.is_add,
                "is_del": self.is_del,
                "is_bg": self.is_bg,
            }
        )


class hunk:
    def __init__(self) -> None:
        self.lines = []

    def add_line(self, l) -> None:
        self.lines.append(l)

    def get_lines(self):
        return self.lines

    def __str__(self) -> str:
        ret = ""
        for l in self.lines:
            ret = ret + "\n" + str(l)
        return ret


class file:
    def __init__(self, fname, is_add, is_del, is_mod, is_rename):
        self.file_name = fname
        self.is_add = is_add
        self.is_del = is_del
        self.is_mod = is_mod
        self.is_rename = is_rename
        self.hunks = []

    def add_hunk(self, h: hunk):
        self.hunks.append(h)

    def get_info(self):
        return {
            "file_name": self.file_name,
            "is_add": self.is_add,
            "is_del": self.is_del,
            "is_mod": self.is_mod,
            "is_rename": self.is_rename,
        }

    def __str__(self) -> str:
        return str(self.get_info())

    def get_hunks(self):
        return self.hunks


class patch:
    def __init__(self, content: str) -> None:
        self.content = content
        self.files = []
        self.re1 = re.compile("diff(\s)+--git(\s)+a/(\S)+(\s)+b/(\S)+")
        self.re2 = re.compile("@@(\s)+-([0-9]*),?(\S)*(\s)+\+([0-9]*),?(\S)*(\s)+@@")
        self.parse()

    def parse_cid(self, line: str) -> str:
        return line.split(" ")[1]

    def parse_author_email(self, line: str):
        author = ""
        for s in line.split(" ")[1:-1]:
            author = author + " " + s
        author = author.strip()
        email = line.split(" ")[-1]
        return author, email

    def parse_time(self, line: str):
        line = line.replace(",", "")
        time_list = line.split(" ")[1:-1]
        time_line = ""
        for t in time_list:
            time_line = time_line + t + " "
        time_line = time_line.strip()
        return strptime(time_line, "%a %d %b %Y %X")

    def parse_commit_msg(self, lines: list):
        cmsg = ""
        i = 0
        for line in lines:
            i = i + 1
            if line.startswith("Subject: [PATCH]"):
                cmsg = cmsg + line[len("Subject: [PATCH]") :]
            elif line == "---":
                return cmsg.strip(), i
            else:
                cmsg = cmsg + "\n" + line

    def parse_hunk(self, lines: list):
        h = hunk()
        for l in lines:
            if l.startswith("-") and not l.startswith("--- a/") and l != ("-- "):
                h.add_line(line(l[1:], False, True, False))
            elif l.startswith("+") and not l.startswith("+++ b/"):
                h.add_line(line(l[1:], True, False, False))
        return h

    def parse_file(self, lines: list):
        is_add = False
        is_del = False
        is_mod = False
        is_rename = False

        f1 = lines[0].split(" ")[-1][2:]
        f2 = lines[0].split(" ")[-2][2:]
        lines = lines[1:]

        if lines[0].startswith("similarity index") and f1 != f2:
            is_rename = True
            return file(f2, is_add, is_del, is_mod, is_rename)
        elif lines[0].startswith("deleted file mode"):
            is_del = True
        elif lines[0].startswith("new file mode"):
            is_add = True
        else:
            is_mod = True

        while len(lines) > 0 and self.re2.match(lines[0]) == None:
            lines = lines[1:]

        hunk_lines = []
        hunks = []

        while len(lines) > 0 and self.re2.match(lines[0]) != None:
            hunk_lines.append(lines[0])
            lines = lines[1:]

            while len(lines) > 0 and self.re2.match(lines[0]) == None:
                hunk_lines.append(lines[0])
                lines = lines[1:]

            hunks.append(self.parse_hunk(hunk_lines))
            hunk_lines = []
            if len(lines) == 0:
                break

        f = file(f2, is_add, is_del, is_mod, is_rename)
        for h in hunks:
            f.add_hunk(h)
        return f

    def parse_files(self, lines: list):
        while len(lines) > 0 and self.re1.match(lines[0]) == None:
            lines = lines[1:]

        file_lines = []
        files = []

        while len(lines) > 0 and self.re1.match(lines[0]) != None:
            file_lines.append(lines[0])
            lines = lines[1:]
            while len(lines) > 0 and self.re1.match(lines[0]) == None:
                file_lines.append(lines[0])
                lines = lines[1:]

            files.append(self.parse_file(file_lines))
            file_lines = []
            if len(lines) == 0:
                break

        return files

    def parse(self) -> None:
        lines = self.content.split("\n")

        while not lines[0].startswith("Subject: [PATCH]"):
            lines = lines[1:]

        # assert lines[0].startswith("Subject: [PATCH]")
        self.cmsg, i = self.parse_commit_msg(lines)
        lines = lines[i:]

        self.files = self.parse_files(lines)

    def get_diff(self):
        diff = ""
        lines = self.content.split("\n")
        while len(lines) > 0 and self.re1.match(lines[0]) == None:
            lines = lines[1:]

        for l in lines:
            if l != "-- ":
                diff = diff + "\n" + l

        return diff.strip()

    def get_author(self):
        return self.author

    def get_commit_id(self):
        return self.commit_id

    def get_email(self):
        return self.email

    def get_date(self):
        return strftime("%Y-%m-%d", self.cdate)

    def get_cmsg(self):
        return self.cmsg

    def get_files(self):
        return self.files


if __name__ == "__main__":
    pass
