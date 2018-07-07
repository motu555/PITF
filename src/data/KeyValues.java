package data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/16.
 */
public class KeyValues<T, S> {
    T key;
    List<S> values;

    public KeyValues(T keyParam, List<S> valuesParam) {
        this.key = keyParam;
        this.values = valuesParam;
    }

    public KeyValues() {
        this.values = new ArrayList<>();
    }

    public T getKey() {
        return key;
    }

    public void setKey(T key) {
        this.key = key;
    }

    public List<S> getValues() {
        return values;
    }

    public void setValues(List<S> values) {
        this.values = values;
    }
}
