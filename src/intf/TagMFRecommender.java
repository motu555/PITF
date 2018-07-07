package intf;

import data.DenseMatrix;
import data.DenseVector;
import data.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangkeqiang on 2016/5/18.
 */
public class TagMFRecommender extends TagRecommender {
    protected int dim;
    protected double initStdev;
    protected int iter;
    protected double learnRate;
    protected double regU;
    protected double regI;
    protected double regT;
    protected DenseMatrix userVectors;
    protected DenseMatrix itemVectors;
    protected DenseMatrix tagUserVectors, tagItemVectors;
    protected  List<DenseVector>measureList;

    public TagMFRecommender(List<Post> trainPostsParam, List<Post> testPostsParam, int dimParam, double initStdevParam, int iterParam,
                            double learnRateParam, double regUParam, double regIParam, double regTParam, int randomSeedParam) throws IOException {
        super(trainPostsParam, testPostsParam, randomSeedParam);
        this.dim = dimParam;
        this.initStdev = initStdevParam;
        this.iter = iterParam;
        this.learnRate = learnRateParam;
        this.regU = regUParam;
        this.regI = regIParam;
        this.regT = regTParam;
    }

    /**
     * initialize recommender model
     */
    protected void initModel() throws Exception {
        this.userVectors = new DenseMatrix(this.numUser, this.dim);
        this.itemVectors = new DenseMatrix(this.numItem, this.dim);
        this.tagUserVectors = new DenseMatrix(this.numTag, this.dim);
        this.tagItemVectors = new DenseMatrix(this.numTag, this.dim);

        this.userVectors.init(0.0, this.initStdev);
        this.itemVectors.init(0.0, this.initStdev);
        this.tagUserVectors.init(0.0, this.initStdev);
        this.tagItemVectors.init(0.0, this.initStdev);

        this.measureList = new ArrayList<>();
    }

    public List<DenseVector> getMeasureList (){
        return measureList;
    }
}
