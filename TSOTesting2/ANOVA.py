#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jan 23 13:37:44 2020

@author: colosu
"""

import scipy.stats as st

table = "results.txt"

file = open(table, "r")    

tso = []
rand = []

for line in file:
    if line != "\hline\n" and not "|" in line and not "Total" in line:
        contents = line.split(' & ')
        tso.append(float(contents[1]))
        rand.append(float(contents[2][:-3]))

# Check Homogeneity of Variance
stat, pvalue = st.levene(tso, rand)
print("Levene-value: " + str(stat))
print("p-value: " + str(pvalue))
print()

if pvalue > 0.1: #ANOVA test
    stat, pvalue = st.f_oneway(tso, rand)
    print("F-value: " + str(stat))
    print("p-value: " + str(pvalue))
    print()
else: #Kruskal-Wallis H-test
    stat, pvalue = st.kruskal(tso, rand, nan_policy='raise')
    print("H-value: " + str(stat))
    print("p-value: " + str(pvalue))
    print()

# t-test
stat, pvalue = st.ttest_ind(tso, rand)
print("T-value: " + str(stat))
print("p-value: " + str(pvalue))