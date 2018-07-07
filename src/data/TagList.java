package data;

import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/16.
 */
public class TagList {
    private int user;
    private int item;
    private List<Integer> tags;


    public TagList(int userParam, int itemParam, List<Integer> tagsParam) {
        this.user = userParam;
        this.item = itemParam;
        this.tags = tagsParam;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }

    public List<Integer> getTags() {
        return tags;
    }

    public void setTags(List<Integer> tags) {
        this.tags = tags;
    }
}

