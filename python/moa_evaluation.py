"""
Created on Mar 31, 2017.

@author: Cornelius Styp von Rekowski (cornelius.styp@ovgu.de)
"""

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.colors as colors
import matplotlib.cm as cmx

def import_task_result(fileName):
    # use first row as column names
    # use first column as index (e.g. cross validation entry id)
    df = pd.read_csv(fileName, sep=',', header=0, index_col=0)
    
    return df

def plot_budget_performance(df):
    groups = df.groupby(by=['learner id', 'fold id'])
    lastEntries = groups.last()
    paramIds = df['learner id'].unique()
    
    # setup plot
    fig = plt.figure()
    ax = fig.add_subplot(111)
    
    # setup color map
    hsv = plt.get_cmap('hsv')
    cNorm = colors.Normalize(vmin=0, vmax=paramIds[-1])
    scalarMap = cmx.ScalarMappable(norm=cNorm, cmap=hsv)
    
    clusters = []
    for p in paramIds:
        folds = lastEntries.loc[p]
        
        x = folds['Rel Number of Label Acquisitions'].values
        y = folds['classifications correct (percent)'].values
        
        c = scalarMap.to_rgba(p)
        
        cluster = ax.plot(x, y, '.', color=c)
        clusters.append(cluster)
    
    plt.show()


if __name__ == '__main__':
    fileName = '../../../Desktop/MOA.csv'
    df = import_task_result(fileName)
    plot_budget_performance(df)
