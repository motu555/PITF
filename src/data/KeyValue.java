package data;

/**
 * Created by wangkeqiang on 2016/5/16.
 */
public class KeyValue<T, S> {
    T key;
    S value;

    public KeyValue(T keyParam, S valueParam) {
        this.key = keyParam;
        this.value = valueParam;
    }

    public T getKey() {
        return key;
    }

    public void setKey(T key) {
        this.key = key;
    }

    public S getValue() {
        return value;
    }

    public void setValue(S value) {
        this.value = value;
    }
}
