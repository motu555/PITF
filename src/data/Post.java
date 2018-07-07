package data;

/**
 * Created by wangkeqiang on 2016/5/14.
 * 这个数据结构帮助把一条用户的行为便于用list存储
 */
public class Post {
    private int user;
    private int item;
    private int tag;
    private long time;
    private int timestamp;

    public Post(int userParam, int itemParam, int tagParam) {
        this.user = userParam;
        this.item = itemParam;
        this.tag = tagParam;
    }

    public Post(int userParam, int itemParam, int tagParam, long timeParam) {
        this.user = userParam;
        this.item = itemParam;
        this.tag = tagParam;
        this.time = timeParam;
    }
    /*public Post(int userParam, int itemParam,  int timestampParam) {
        this.user = userParam;
        this.item = itemParam;
        this.timestamp = timestampParam;
    }*/

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

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
    /*public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }*/
}
