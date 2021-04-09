#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jan 21 13:39:02 2020

@author: colosu
"""

n = 3
tables = []
files = []
contents = []

for i in range(1,n+1):
    tables.append("results" + str(i) + ".txt")

for i in range(n):
    files.append(open(tables[i], "r"))    

for i in range(n):
    contents.append([])
    for line in files[i]:
        if line != "\hline\n":
            contents[i].append(line)

result = open("table.txt", "w")
for i in range(1,len(contents[0])):
    line = str(i)
    for j in range(n):
        line += contents[j][i][contents[j][i].find("&")-1:-3]
    result.write(line + "\\\\\n")
    result.write("\hline\n")

result.close()

for i in range(n):
    files[i].close()