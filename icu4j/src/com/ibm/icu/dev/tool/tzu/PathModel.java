/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.text.JTextComponent;

/**
 * Represents a list of IncludePaths that is usable by any class that uses
 * AbstractListModels (such as a JList in swing). Also contains methods to begin
 * a search on those paths using ICUJarFinder and placing the results in a
 * ResultModel, and methods to load a path list from a file.
 */
class PathModel extends AbstractListModel {
    public PathModel(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns an iterator of the path list.
     */
    public Iterator iterator() {
        return list.iterator();
    }

    /**
     * Gets the index of the path list.
     */
    public Object getElementAt(int index) {
        return list.get(index);
    }

    /**
     * Gets the size of the path list.
     */
    public int getSize() {
        return (list == null) ? 0 : list.size();
    }

    /**
     * Adds a filename to the path list if it is valid and unique. The filename
     * must either be of the form (<b>+</b>|<b>-</b>)<i>pathstring</i> and
     * exist, or of the form <b>all</b>. In the case of the latter, all drives
     * are added to the path list.
     * 
     * @param includeFilename
     *            A filename in the form above.
     * @return Whether or not <code>includeFilename</code> is both of the form
     *         detailed above and exists.
     */
    public boolean add(String includeFilename) {
        if ("all".equals(includeFilename)) {
            logger
                    .printlnToScreen("The tool will search all drives for ICU4J jars except any excluded directories specified");
            addAllDrives();
            return true;
        }

        return add(new IncludePath(new File(includeFilename.substring(1).trim()), includeFilename.charAt(0) == '+'));
    }

    /**
     * Adds an IncludePath to the path list if it exists and is unique.
     * 
     * @param path
     *            An existing path.
     * @return Whether or not the given IncludePath exists.
     */
    public boolean add(IncludePath path) {
        remove(path);

        if (path.getPath().exists()) {
            list.add(path);
            int index = list.size() - 1;
            fireIntervalAdded(this, index, index);
            return true;
        }

        return false;
    }

    /**
     * Adds all drives to the path list.
     */
    public void addAllDrives() {
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++)
            add(new IncludePath(roots[i], true));
    }

    /**
     * Removes a path from the path list. Since there are no duplicates in the
     * path list, this method either removes a single path or removes none.
     * 
     * @param path
     * @return Whether or not a path was removed.
     */
    public boolean remove(IncludePath path) {
        int index = list.indexOf(path);
        if (index != -1) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
            return true;
        }

        return false;
    }

    /**
     * Removes a selection of paths from the path list by index.
     * 
     * @param indices
     *            The indices of the path list to remove.
     */
    public void remove(int[] indices) {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int max = indices[indices.length - 1];
            int min = indices[0];
            for (int i = indices.length - 1; i >= 0; i--)
                list.remove(indices[i]);
            fireIntervalRemoved(this, min, max);
        }
    }

    /**
     * Clears the path list.
     */
    public void removeAll() {
        if (list.size() > 0) {
            int index = list.size() - 1;
            list.clear();
            fireIntervalRemoved(this, 0, index);
        }
    }

    /**
     * Searches a selection of paths in the path list for updatable ICU4J jars.
     * Results are added to the result model. The indices provided are the
     * indices of the path list to search.
     * 
     * @param resultModel
     *            The result model to store the results of the search.
     * @param indices
     *            The indices of the path list to use in the search.
     * @param subdirs
     *            Whether to search subdiretories.
     * @param backupDir
     *            Where to store backup files.
     * @param statusBar
     *            The status bar to dispay status.
     * @throws InterruptedException
     */
    public void search(ResultModel resultModel, int[] indices, boolean subdirs, File backupDir, JTextComponent statusBar)
            throws InterruptedException {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;
            IncludePath[] paths = new IncludePath[n];

            int k = 0;
            Iterator iter = list.iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    paths[k++] = (IncludePath) iter.next();
                else
                    iter.next();

            ICUJarFinder.search(resultModel, logger, statusBar, paths, subdirs, backupDir);
        }
    }

    /**
     * Searches each path in the path list for updatable ICU4J jars. Results are
     * added to the result model.
     * 
     * @param resultModel
     *            The result model to store the results of the search.
     * @param subdirs
     *            Whether to search subdiretories.
     * @param backupDir
     *            Where to store backup files.
     * @param statusBar
     *            The status bar to dispay status.
     * @throws InterruptedException
     */
    public void searchAll(ResultModel resultModel, boolean subdirs, File backupDir, JTextComponent statusBar)
            throws InterruptedException {
        if (list.size() > 0) {
            int n = list.size();
            IncludePath[] paths = new IncludePath[n];
            Iterator iter = list.iterator();
            for (int i = 0; i < n; i++)
                paths[i] = (IncludePath) iter.next();
            ICUJarFinder.search(resultModel, logger, statusBar, paths, subdirs, backupDir);
        }
    }

    /**
     * Loads a list of paths from <code>PATHLIST_FILENAME</code>. Each path
     * must be of the form
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void loadPaths() throws IOException, IllegalArgumentException {
        logger.printlnToScreen("Scanning " + PATHLIST_FILENAME + " file...");
        logger.printlnToScreen(PATHLIST_FILENAME + " file contains");

        BufferedReader reader = null;
        int lineNumber = 1;
        String line;
        char sign;

        try {
            reader = new BufferedReader(new FileReader(PATHLIST_FILENAME));
            while (reader.ready()) {
                line = reader.readLine().trim();

                if (line.length() >= 1) {
                    sign = line.charAt(0);
                    if (sign != '#') {
                        logger.printlnToScreen(line);
                        if (sign != '+' && sign != '-' && !"all".equals(line))
                            pathListError("Each path entry must start with a + or - to denote inclusion/exclusion",
                                    lineNumber);
                        if (!add(line))
                            pathListError("\"" + line.substring(1).trim()
                                    + "\" is not a valid file or directory (perhaps it does not exist?)", lineNumber);
                    }
                }

                lineNumber++;
            }
        } catch (FileNotFoundException ex) {
            pathListError("The " + PATHLIST_FILENAME + " file doesn't exist.");
        } catch (IOException ex) {
            pathListError("Could not read the " + PATHLIST_FILENAME + " file.");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Throws an IllegalArgumentException with the specified message and line
     * number.
     * 
     * @param message
     *            The message to put in the exception.
     * @param lineNumber
     *            The line number to put in the exception.
     * @throws IllegalArgumentException
     */
    private static void pathListError(String message, int lineNumber) throws IllegalArgumentException {
        throw new IllegalArgumentException("Error in " + PATHLIST_FILENAME + " (line " + lineNumber + "): " + message);
    }

    /**
     * Throws an IOException with the specified message.
     * 
     * @param message
     *            The message to put in the exception.
     * @throws IOException
     */
    private static void pathListError(String message) throws IOException {
        throw new IOException("Error in " + PATHLIST_FILENAME + ": " + message);
    }

    public static final String PATHLIST_FILENAME = "DirectorySearch.txt";

    public static final long serialVersionUID = 1337;

    private List list = new ArrayList(); // list of paths (Files)

    private Logger logger;
}
