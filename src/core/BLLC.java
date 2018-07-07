package core;

import data.Post;
import intf.TagRecommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangkeqiang on 2016/5/18.
 * 基线方法：BLL+MPm
 */
public class BLLC extends TagRecommender {
    double d = 0.5;
    double beta = 0.5;
    List<Map<Integer, List>> userTagTimeList;
    List<Map<Integer, Integer>> itemTagCountList;//有关频率的计算
    List<Long> userTimeList;

    List<Map<Integer, Double>> userTagPrefList;
    List<Map<Integer, Double>> itemTagPrefList;

    public BLLC(List<Post> trainPostsParam, List<Post> testPostsParam, int randomSeedParam, double dParam, double betaParam) throws IOException {
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

        for (Post post : this.trainPosts) {
            int user = post.getUser();
            int item = post.getItem();
            int tag = post.getTag();
            long time = post.getTime();

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
        }

        for (Post post : this.testPosts) {
            int user = post.getUser();
            long time = post.getTime();

            if (time > this.userTimeList.get(user)) {
                this.userTimeList.set(user, time);
            }
        }
    }

    @Override
    protected void buildModel() throws Exception {
        this.trainStartTime = System.currentTimeMillis();
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
        this.trainEndTime = System.currentTimeMillis();
    }

    @Override
    protected double predict(int user, int item, int tag) {
        double userTagWeight = 0.0;
        double itemTagWeight = 0.0;
        if (this.userTagPrefList.get(user).containsKey(tag)) {
            userTagWeight = this.userTagPrefList.get(user).get(tag);
        }
        if (this.itemTagPrefList.get(item).containsKey(tag)) {
            itemTagWeight = this.itemTagPrefList.get(item).get(tag);
        }
        return this.beta * userTagWeight + (1 - this.beta) * itemTagWeight;
    }
}
