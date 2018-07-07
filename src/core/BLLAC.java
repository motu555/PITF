package core;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import data.Post;
import data.SparseMatrix;
import intf.TagRecommender;
import util.Lists;

import java.io.IOException;
import java.util.*;

/**
 * Created by wangkeqiang on 2016/5/18.
 * 基线方法：BLL_AC + MPm
 * <p>
 * 我们的数据中时间戳是ms
 * <p>
 * 时间间隔的单位设置：
 * 原论文BLL代码https://github.com/learning-layers/TagRec/使用的是两者时间戳的差值+1.0，且时间戳以秒为单位。
 * 而此处我们与之一致，因为用的是毫秒： + 1000ms
 */
public class BLLAC extends TagRecommender {
    double d = 0.5;
    double beta = 0.5;
    List<Map<Integer, List>> userTagTimeList;
    List<Map<Integer, Integer>> itemTagCountList;
    List<Long> userTimeList;

    List<Map<Integer, Double>> userTagPrefList;
    List<Map<Integer, Double>> itemTagPrefList;
    protected Table<Integer, Integer, Set<Integer>> trainTagTableSet;

    SparseMatrix tagSMatrix, itemTagWMatrix;
    int[] tagCountVectors;

    public BLLAC(List<Post> trainPostsParam, List<Post> testPostsParam, int randomSeedParam, double dParam, double betaParam) throws IOException {
        super(trainPostsParam, testPostsParam, randomSeedParam);
        this.d = dParam;
        this.beta = betaParam;

    }

    protected void initModel() {
        this.userTagTimeList = new ArrayList<>();
        this.itemTagCountList = new ArrayList<>();
        this.userTimeList = new ArrayList<>();
        this.userTagPrefList = new ArrayList<>();
        this.itemTagPrefList = new ArrayList<>();

        for (int userIndex = 0; userIndex < this.numUser; userIndex++) {
            this.userTagTimeList.add(new HashMap<>());
            this.userTimeList.add(0L);
            this.userTagPrefList.add(new HashMap<>());
        }

        for (int itemIndex = 0; itemIndex < this.numItem; itemIndex++) {
            this.itemTagCountList.add(new HashMap<>());
            this.itemTagPrefList.add(new HashMap<>());
        }

        trainTagTableSet = HashBasedTable.create();
        Table<Integer, Integer, Integer> itemTagTable = HashBasedTable.create();
        tagCountVectors = new int[this.numTag];
        for (Post post : this.trainPosts) {
            int user = post.getUser();
            int item = post.getItem();
            int tag = post.getTag();
            long time = post.getTime();
            tagCountVectors[tag]++;

            if (!itemTagTable.contains(item, tag)) {
                itemTagTable.put(item, tag, 0);
            }
            itemTagTable.put(item, tag, itemTagTable.get(item, tag) + 1);

            if (time > this.userTimeList.get(user)) {
                this.userTimeList.set(user, time);
            }
            if (!this.userTagTimeList.get(user).containsKey(tag)) {
                this.userTagTimeList.get(user).put(tag, new ArrayList<Long>());
            }
            this.userTagTimeList.get(user).get(tag).add(time);

            if (!this.itemTagCountList.get(item).containsKey(tag)) {
                this.itemTagCountList.get(item).put(tag, 1);
            } else {
                this.itemTagCountList.get(item).put(tag, this.itemTagCountList.get(item).get(tag) + 1);
            }

            if (!this.trainTagTableSet.contains(user, item)) {
                this.trainTagTableSet.put(user, item, new HashSet<>());
            }
            this.trainTagTableSet.get(user, item).add(tag);
        }

        for (Post post : this.testPosts) {
            int user = post.getUser();
            long time = post.getTime();

            if (time > this.userTimeList.get(user)) {
                this.userTimeList.set(user, time);
            }
        }

        Table<Integer, Integer, Double> tagSTable = HashBasedTable.create();
        for (Table.Cell<Integer, Integer, Set<Integer>> tempCell : this.trainTagTableSet.cellSet()) {
            for (int tagx : tempCell.getValue()) {
                for (int tagy : tempCell.getValue()) {
                    if (!tagSTable.contains(tagx, tagy)) {
                        tagSTable.put(tagx, tagy, 0.0d);
                    }
                    tagSTable.put(tagx, tagy, tagSTable.get(tagx, tagy) + 1.0d);
                }
            }
        }
        tagSMatrix = new SparseMatrix(numTag, numTag, tagSTable);
        itemTagWMatrix = new SparseMatrix(numItem, numTag, itemTagTable);

        for (int tagx = 0; tagx < this.numTag; tagx++) {
            int xCount = this.tagCountVectors[tagx];
            for (int tagy : tagSMatrix.getColumns(tagx)) {
                int yCount = this.tagCountVectors[tagy];
                double coocurenceCount = this.tagSMatrix.get(tagx, tagy);
                this.tagSMatrix.set(tagx, tagy, coocurenceCount / (xCount + yCount - coocurenceCount));
            }
        }
    }

    @Override
    protected void buildModel() throws Exception {
        for (int user = 0; user < this.numUser; user++) {
            double nor = 0.0d;
            long preTime = this.userTimeList.get(user);
            Map<Integer, List> tempTagsTimeMap = this.userTagTimeList.get(user);

            for (Map.Entry<Integer, List> tempTagsTime : tempTagsTimeMap.entrySet()) {
                int tag = tempTagsTime.getKey();
                List<Long> tempTimeList = tempTagsTime.getValue();
                double tempValue = 0.0;
                for (long tempTime : tempTimeList) {
                    tempValue += Math.pow(1.0 * (preTime - tempTime + 1000), -this.d);
                }
                nor += tempValue;
                this.userTagPrefList.get(user).put(tag, tempValue);
            }

            for (int tag : tempTagsTimeMap.keySet()) {
                this.userTagPrefList.get(user).put(tag, this.userTagPrefList.get(user).get(tag) / nor);

            }
        }

        for (int item = 0; item < this.numItem; item++) {
            double nor = 0.0;
            Map<Integer, Integer> tempTagsCountMap = this.itemTagCountList.get(item);
            for (Map.Entry<Integer, Integer> tempTagsCount : tempTagsCountMap.entrySet()) {
                int tag = tempTagsCount.getKey();
                int count = tempTagsCount.getValue();
                double tempValue = count;
                nor += tempValue;
                this.itemTagPrefList.get(item).put(tag, tempValue);
            }

            for (int tag : tempTagsCountMap.keySet()) {
                this.itemTagPrefList.get(item).put(tag, this.itemTagPrefList.get(item).get(tag) / nor);
            }
        }
    }

    @Override
    protected double predict(int user, int item, int tag) {
        double userTagWeight = 0.0;
        double itemTagWeight = 0.0;
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            itemTagWeight = this.itemTagPrefList.get(item).get(tag);
        }
        return this.beta * userTagWeight + (1 - this.beta) * itemTagWeight;
    }

    protected List<Map.Entry<Integer, Double>> predictTopN(int user, int item) throws Exception {
        List<Map.Entry<Integer, Double>> itemScores = new ArrayList<>();
        double[] tagA = new double[this.numTag];
        double nor = 0.0d;
        for (int tag = 0; tag < this.numTag; tag++) {
            if (this.userTagPrefList.get(user).containsKey(tag)) {
                double val = this.tagSMatrix.row(tag).inner(this.itemTagWMatrix.row(item)) + this.userTagPrefList.get(user).get(tag);
                tagA[tag] += val;
                nor += val;
            }
        }

        for (int tag = 0; tag < this.numTag; tag++) {
            if (this.userTagPrefList.get(user).containsKey(tag)) {
                tagA[tag] /= nor;
            }
        }
        for (int tag = 0; tag < this.numTag; tag++) {
            double rank = this.predict(user, item, tag);
            rank += this.beta * tagA[tag];
            itemScores.add(new AbstractMap.SimpleImmutableEntry<>(tag, rank));
        }
        Lists.sortList(itemScores, true);
        return itemScores;
    }
}
