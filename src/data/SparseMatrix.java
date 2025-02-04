// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package data;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import util.Stats;

import java.io.Serializable;
import java.util.*;

/**
 * Data Structure: Sparse Matrix whose implementation is modified from M4J library
 * <p>
 * <ul>
 * <li><a href="http://netlib.org/linalg/html_templates/node91.html">Compressed Row Storage (CRS)</a></li>
 * <li><a href="http://netlib.org/linalg/html_templates/node92.html">Compressed Col Storage (CCS)</a></li>
 * </ul>
 *
 * @author guoguibing
 */
public class SparseMatrix implements Iterable<MatrixEntry>, Serializable {

    private static final long serialVersionUID = 8024536511172609539L;

    // matrix dimension
    protected int numRows, numColumns;

    // Compressed Row Storage (CRS)
    protected double[] rowData;
    protected int[] rowPtr, colInd;

    // Compressed Col Storage (CCS)
    protected double[] colData;
    protected int[] colPtr, rowInd;

    /**
     * Construct a sparse matrix with both CRS and CCS structures
     */
    public SparseMatrix(int rows, int cols, Table<Integer, Integer, ? extends Number> dataTable,
                        Multimap<Integer, Integer> colMap) {
        numRows = rows;
        numColumns = cols;

        construct(dataTable, colMap);
    }

    /**
     * Construct a sparse matrix with only CRS structures
     */
    public SparseMatrix(int rows, int cols, Table<Integer, Integer, ? extends Number> dataTable) {
        this(rows, cols, dataTable, null);
    }

    /**
     * Define a sparse matrix without data, only use for {@code transpose} method
     */
    private SparseMatrix(int rows, int cols) {
        numRows = rows;
        numColumns = cols;
    }

    /**
     * Construct a sparse matrix from another sparse matrix
     *
     * @param mat the original sparse matrix
     */
    public SparseMatrix(SparseMatrix mat) {
        numRows = mat.numRows;
        numColumns = mat.numColumns;

        copyCRS(mat.rowData, mat.rowPtr, mat.colInd);

        copyCCS(mat.colData, mat.colPtr, mat.rowInd);
    }

    private void copyCRS(double[] data, int[] ptr, int[] idx) {
        rowData = new double[data.length];
        for (int i = 0; i < rowData.length; i++)
            rowData[i] = data[i];

        rowPtr = new int[ptr.length];
        for (int i = 0; i < rowPtr.length; i++)
            rowPtr[i] = ptr[i];

        colInd = new int[idx.length];
        for (int i = 0; i < colInd.length; i++)
            colInd[i] = idx[i];
    }

    private void copyCCS(double[] data, int[] ptr, int[] idx) {

        colData = new double[data.length];
        for (int i = 0; i < colData.length; i++)
            colData[i] = data[i];

        colPtr = new int[ptr.length];
        for (int i = 0; i < colPtr.length; i++)
            colPtr[i] = ptr[i];

        rowInd = new int[idx.length];
        for (int i = 0; i < rowInd.length; i++)
            rowInd[i] = idx[i];
    }

    /**
     * Make a deep clone of current matrix
     */
    public SparseMatrix clone() {
        return new SparseMatrix(this);
    }

    /**
     * @return the transpose of current matrix
     */
    public SparseMatrix transpose() {
        SparseMatrix tr = new SparseMatrix(numColumns, numRows);

        tr.copyCRS(this.rowData, this.rowPtr, this.colInd);
        tr.copyCCS(this.colData, this.colPtr, this.rowInd);

        return tr;
    }

    /**
     * @return the row pointers of CRS structure
     */
    public int[] getRowPointers() {
        return rowPtr;
    }

    /**
     * @return the column indices of CCS structure
     */
    public int[] getColumnIndices() {
        return colInd;
    }

    /**
     * @return the cardinary of current matrix
     */
    public int size() {
        int size = 0;

        for (MatrixEntry me : this)
            if (me.get() != 0)
                size++;

        return size;
    }

    /**
     * @return the data table of this matrix as (row, column, value) cells
     */
    public Table<Integer, Integer, Double> getDataTable() {
        Table<Integer, Integer, Double> res = HashBasedTable.create();

        for (MatrixEntry me : this) {
            if (me.get() != 0)
                res.put(me.row(), me.column(), me.get());
        }

        return res;
    }

    /**
     * Construct a sparse matrix
     *
     * @param dataTable       data table
     * @param columnStructure column structure
     */
    private void construct(Table<Integer, Integer, ? extends Number> dataTable,
                           Multimap<Integer, Integer> columnStructure) {
        int nnz = dataTable.size();

        // CRS
        rowPtr = new int[numRows + 1];
        colInd = new int[nnz];
        rowData = new double[nnz];

        int j = 0;
        for (int i = 1; i <= numRows; ++i) {
            Set<Integer> cols = dataTable.row(i - 1).keySet();
            rowPtr[i] = rowPtr[i - 1] + cols.size();

            for (int col : cols) {
                colInd[j++] = col;
                if (col < 0 || col >= numColumns)
                    throw new IllegalArgumentException("colInd[" + j + "]=" + col
                            + ", which is not a valid column index");
            }

            Arrays.sort(colInd, rowPtr[i - 1], rowPtr[i]);
        }

        // CCS
        colPtr = new int[numColumns + 1];
        rowInd = new int[nnz];
        colData = new double[nnz];

        j = 0;
        for (int i = 1; i <= numColumns; ++i) {
            // dataTable.col(i-1) is more time-consuming than columnStructure.get(i-1)
            Collection<Integer> rows = columnStructure != null ? columnStructure.get(i - 1) : dataTable.column(i - 1)
                    .keySet();
            colPtr[i] = colPtr[i - 1] + rows.size();

            for (int row : rows) {
                rowInd[j++] = row;
                if (row < 0 || row >= numRows)
                    throw new IllegalArgumentException("rowInd[" + j + "]=" + row + ", which is not a valid row index");
            }

            Arrays.sort(rowInd, colPtr[i - 1], colPtr[i]);
        }

        // set data
        for (Cell<Integer, Integer, ? extends Number> en : dataTable.cellSet()) {
            int row = en.getRowKey();
            int col = en.getColumnKey();
            double val = en.getValue().doubleValue();

            set(row, col, val);
        }
    }

    /**
     * @return number of rows
     */
    public int numRows() {
        return numRows;
    }

    /**
     * @return number of columns
     */
    public int numColumns() {
        return numColumns;
    }

    /**
     * @return referce to the data of current matrix
     */
    public double[] getData() {
        return rowData;
    }

    /**
     * Set a value to entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @param val    value to set
     */
    public void set(int row, int column, double val) {
        int index = getCRSIndex(row, column);
        rowData[index] = val;

        index = getCCSIndex(row, column);
        colData[index] = val;
    }

    /**
     * Add a value to entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @param val    value to add
     */
    public void add(int row, int column, double val) {
        int index = getCRSIndex(row, column);
        rowData[index] += val;

        index = getCCSIndex(row, column);
        colData[index] += val;
    }

    /**
     * Retrieve value at entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @return value at entry [row, column]
     */
    public double get(int row, int column) {

        int index = Arrays.binarySearch(colInd, rowPtr[row], rowPtr[row + 1], column);

        if (index >= 0)
            return rowData[index];
        else
            return 0;
    }

    /**
     * get a row sparse vector of a matrix
     *
     * @param row row id
     * @return a sparse vector of {index, value}
     */
    public SparseVector row(int row) {

        SparseVector sv = new SparseVector(numColumns);

        if (row < numRows) {
            for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
                int col = colInd[j];
                double val = get(row, col);
                if (val != 0.0)
                    sv.set(col, val);
            }
        } // return an empty vector if the row does not exist in training matrix

        return sv;
    }

    /**
     * get columns of a specific row where (row, column) entries are non-zero
     *
     * @param row row id
     * @return a list of column index
     */
    public List<Integer> getColumns(int row) {
        List<Integer> res = new ArrayList<>();

        if (row < numRows) {
            for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
                int col = colInd[j];
                double val = get(row, col);
                if (val != 0.0)
                    res.add(col);
            }
        }

        return res;
    }

    /**
     * create a row cache of a matrix in {row, row-specific vector}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific vector}
     */
    public LoadingCache<Integer, SparseVector> rowCache(String cacheSpec) {
        LoadingCache<Integer, SparseVector> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, SparseVector>() {

                    @Override
                    public SparseVector load(Integer rowId) throws Exception {
                        return row(rowId);
                    }
                });

        return cache;
    }

    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    public LoadingCache<Integer, List<Integer>> rowColumnsCache(String cacheSpec) {
        LoadingCache<Integer, List<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, List<Integer>>() {

                    @Override
                    public List<Integer> load(Integer rowId) throws Exception {
                        return getColumns(rowId);
                    }
                });

        return cache;
    }

    /**
     * create a column cache of a matrix
     *
     * @param cacheSpec cache specification
     * @return a matrix column cache
     */
    public LoadingCache<Integer, SparseVector> columnCache(String cacheSpec) {
        LoadingCache<Integer, SparseVector> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, SparseVector>() {

                    @Override
                    public SparseVector load(Integer columnId) throws Exception {
                        return column(columnId);
                    }
                });

        return cache;
    }

    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    public LoadingCache<Integer, List<Integer>> columnRowsCache(String cacheSpec) {
        LoadingCache<Integer, List<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, List<Integer>>() {

                    @Override
                    public List<Integer> load(Integer colId) throws Exception {
                        return getRows(colId);
                    }
                });

        return cache;
    }

    /**
     * get a row sparse vector of a matrix
     *
     * @param row    row id
     * @param except row id to be excluded
     * @return a sparse vector of {index, value}
     */
    public SparseVector row(int row, int except) {

        SparseVector sv = new SparseVector(numColumns);

        for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
            int col = colInd[j];
            if (col != except) {
                double val = get(row, col);
                if (val != 0.0)
                    sv.set(col, val);
            }
        }
        return sv;
    }

    /**
     * query the size of a specific row
     *
     * @param row row id
     * @return the size of non-zero elements of a row
     */
    public int rowSize(int row) {

        int size = 0;
        for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
            int col = colInd[j];
            if (get(row, col) != 0.0)
                size++;
        }

        return size;
    }

    /**
     * @return a list of rows which have at least one non-empty entry
     */
    public List<Integer> rows() {
        List<Integer> list = new ArrayList<>();

        for (int row = 0; row < numRows; row++) {
            for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
                int col = colInd[j];
                if (get(row, col) != 0.0) {
                    list.add(row);
                    break;
                }
            }
        }

        return list;
    }

    /**
     * get a col sparse vector of a matrix
     *
     * @param col col id
     * @return a sparse vector of {index, value}
     */
    public SparseVector column(int col) {

        SparseVector sv = new SparseVector(numRows);

        if (col < numColumns) {
            for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
                int row = rowInd[j];
                double val = get(row, col);
                if (val != 0.0)
                    sv.set(row, val);
            }
        } // return an empty vector if the column does not exist in training
        // matrix

        return sv;
    }

    /**
     * query the size of a specific col
     *
     * @param col col id
     * @return the size of non-zero elements of a row
     */
    public int columnSize(int col) {

        int size = 0;

        for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
            int row = rowInd[j];
            double val = get(row, col);
            if (val != 0.0)
                size++;
        }

        return size;
    }

    /**
     * get rows of a specific column where (row, column) entries are non-zero
     *
     * @param col column id
     * @return a list of column index
     */
    public List<Integer> getRows(int col) {

        List<Integer> res = new ArrayList<>();

        if (col < numColumns) {
            for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
                int row = rowInd[j];
                double val = get(row, col);
                if (val != 0.0)
                    res.add(row);
            }
        }

        return res;
    }

    /**
     * @return a list of columns which have at least one non-empty entry
     */
    public List<Integer> columns() {
        List<Integer> list = new ArrayList<>();

        for (int col = 0; col < numColumns; col++) {
            for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
                int row = rowInd[j];
                double val = get(row, col);
                if (val != 0.0) {
                    list.add(col);
                    break;
                }
            }
        }

        return list;
    }

    /**
     * @return sum of matrix data
     */
    public double sum() {
        return Stats.sum(rowData);
    }

    /**
     * @return mean of matrix data
     */
    public double mean() {
        return sum() / size();
    }

    /**
     * Normalize the matrix entries to (0, 1) by (x-min)/(max-min)
     *
     * @param min minimum value
     * @param max maximum value
     */
    public void normalize(double min, double max) {
        assert max > min;

        for (MatrixEntry me : this) {
            double entry = me.get();
            if (entry != 0)
                me.set((entry - min) / (max - min));
        }
    }

    /**
     * Normalize the matrix entries to (0, 1) by (x/max)
     *
     * @param max maximum value
     */
    public void normalize(double max) {
        normalize(0, max);
    }

    /**
     * Standardize the matrix entries by row- or column-wise z-scores (z=(x-u)/sigma)
     *
     * @param isByRow standardize by row if true; otherwise by column
     */
    public void standardize(boolean isByRow) {

        int iters = isByRow ? numRows : numColumns;
        for (int iter = 0; iter < iters; iter++) {
            SparseVector vec = isByRow ? row(iter) : column(iter);

            if (vec.getCount() > 0) {

                double[] data = vec.getData();
                double mu = Stats.mean(data);
                double sigma = Stats.sd(data, mu);

                for (VectorEntry ve : vec) {
                    int idx = ve.index();
                    double val = ve.get();
                    double z = (val - mu) / sigma;

                    if (isByRow)
                        this.set(iter, idx, z);
                    else
                        this.set(idx, iter, z);
                }
            }
        }
    }

    /**
     * remove zero entries of the given matrix
     */
    public static void reshape(SparseMatrix mat) {

        SparseMatrix res = new SparseMatrix(mat.numRows, mat.numColumns);
        int nnz = mat.size();

        // Compressed Row Storage (CRS)
        res.rowData = new double[nnz];
        res.colInd = new int[nnz];
        res.rowPtr = new int[mat.numRows + 1];

        // handle row data
        int index = 0;
        for (int i = 1; i < mat.rowPtr.length; i++) {

            for (int j = mat.rowPtr[i - 1]; j < mat.rowPtr[i]; j++) {
                // row i-1, row 0 always starts with 0

                double val = mat.rowData[j];
                int col = mat.colInd[j];
                if (val != 0) {
                    res.rowData[index] = val;
                    res.colInd[index] = col;

                    index++;
                }
            }
            res.rowPtr[i] = index;

        }

        // Compressed Col Storage (CCS)
        res.colData = new double[nnz];
        res.rowInd = new int[nnz];
        res.colPtr = new int[mat.numColumns + 1];

        // handle column data
        index = 0;
        for (int j = 1; j < mat.colPtr.length; j++) {
            for (int i = mat.colPtr[j - 1]; i < mat.colPtr[j]; i++) {
                // column j-1, index i

                double val = mat.colData[i];
                int row = mat.rowInd[i];
                if (val != 0) {
                    res.colData[index] = val;
                    res.rowInd[index] = row;

                    index++;
                }
            }
            res.colPtr[j] = index;
        }

        // write back to the given matrix, note that here mat is just a reference copy of the original matrix
        mat.rowData = res.rowData;
        mat.colInd = res.colInd;
        mat.rowPtr = res.rowPtr;

        mat.colData = res.colData;
        mat.rowInd = res.rowInd;
        mat.colPtr = res.colPtr;
    }

    /**
     * @return a new matrix with shape (rows, cols) with data from the current matrix
     */
    public SparseMatrix reshape(int rows, int cols) {

        Table<Integer, Integer, Double> data = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        int rowIndex, colIndex;
        for (int i = 1; i < rowPtr.length; i++) {
            for (int j = rowPtr[i - 1]; j < rowPtr[i]; j++) {
                int row = i - 1;
                int col = colInd[j];
                double val = rowData[j]; // (row, col, val)

                if (val != 0) {
                    int oldIndex = row * numColumns + col;

                    rowIndex = oldIndex / cols;
                    colIndex = oldIndex % cols;

                    data.put(rowIndex, colIndex, val);
                    colMap.put(colIndex, rowIndex);
                }
            }
        }

        return new SparseMatrix(rows, cols, data, colMap);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d\t%d\t%d\n", new Object[]{numRows, numColumns, size()}));

        for (MatrixEntry me : this)
            if (me.get() != 0)
                sb.append(String.format("%d\t%d\t%f\n", new Object[]{me.row(), me.column(), me.get()}));

        return sb.toString();
    }

    /**
     * @return a matrix format string
     */
    public String matString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dimension: ").append(numRows).append(" x ").append(numColumns).append("\n");

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                sb.append(get(i, j));
                if (j < numColumns - 1)
                    sb.append("\t");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Finds the insertion index of CRS
     */
    private int getCRSIndex(int row, int col) {
        int i = Arrays.binarySearch(colInd, rowPtr[row], rowPtr[row + 1], col);

        if (i >= 0 && colInd[i] == col)
            return i;
        else
            throw new IndexOutOfBoundsException("Entry (" + (row + 1) + ", " + (col + 1)
                    + ") is not in the matrix structure");
    }

    /**
     * Finds the insertion index of CCS
     */
    private int getCCSIndex(int row, int col) {
        int i = Arrays.binarySearch(rowInd, colPtr[col], colPtr[col + 1], row);

        if (i >= 0 && rowInd[i] == row)
            return i;
        else
            throw new IndexOutOfBoundsException("Entry (" + (row + 1) + ", " + (col + 1)
                    + ") is not in the matrix structure");
    }

    public Iterator<MatrixEntry> iterator() {
        return new MatrixIterator();
    }

    /**
     * Entry of a compressed row matrix
     */
    private class SparseMatrixEntry implements MatrixEntry {

        private int row, cursor;

        /**
         * Updates the entry
         */
        public void update(int row, int cursor) {
            this.row = row;
            this.cursor = cursor;
        }

        public int row() {
            return row;
        }

        public int column() {
            return colInd[cursor];
        }

        public double get() {
            return rowData[cursor];
        }

        public void set(double value) {
            rowData[cursor] = value;
        }
    }

    private class MatrixIterator implements Iterator<MatrixEntry> {

        private int row, cursor;

        private SparseMatrixEntry entry = new SparseMatrixEntry();

        public MatrixIterator() {
            // Find first non-empty row
            nextNonEmptyRow();
        }

        /**
         * Locates the first non-empty row, starting at the current. After the new row has been found, the cursor is
         * also updated
         */
        private void nextNonEmptyRow() {
            while (row < numRows && rowPtr[row] == rowPtr[row + 1])
                row++;
            cursor = rowPtr[row];
        }

        public boolean hasNext() {
            return cursor < rowData.length;
        }

        public MatrixEntry next() {
            entry.update(row, cursor);

            // Next position is in the same row
            if (cursor < rowPtr[row + 1] - 1)
                cursor++;

                // Next position is at the following (non-empty) row
            else {
                row++;
                nextNonEmptyRow();
            }

            return entry;
        }

        public void remove() {
            entry.set(0);
        }

    }
}
