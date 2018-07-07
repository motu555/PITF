package dataProcess;

import java.io.*;
import java.util.*;

/**
 * Created by motu on 2018/6/26.
 * 处理后的数据是user,restaurant,category,time，每一条表示用户，餐厅，种类，签到时间
 *
 */
public class Filter {
    /**
     * shopInfo.json中的统计信息
     */
    static int originShopNum;
    /**
     * 原始的shopReviewExtend.json？？中的统计信息
     */
    static int originReviewCount;
    static Set<String> originUserSet;
    static Set<String> originShopSet;
    /**
     * shopReviewExtend.json与shopInfo.json取交集，并经过一定条件过滤后的统计信息
     */
    static int tripleNum;
    static int reviewCount;
    static Set<String> userSet = new HashSet<>();
    static Set<String> shopSet = new HashSet<>();
    /**
     * !!过滤评论数据会用到的数据结构
     */
    //结构需要修改！！
    static Map<String, Map<String, Integer>> checkInRecordMap; //user-<shop, category>-time record
    static Map<String, Set<String>> globalUserItemSetMap; // <user,<shop>>
    //static List<String> categoryFilterList;
    static public Map<String, String> shopCategoryMap;


    /**
     * 路径配置
     */
    public static String rootPath = "/Users/motu/Documents/！毕业论文/PITF/rawdata/";
    public static String shopInfoPath = rootPath + "";
    public static String desPath = rootPath + "";
    public static String reviewPath = rootPath +"checkinWithTimestamp.txt";

    public static void main(String[] args) {
        System.out.println("start " + new Date());
        int userLeastCount = 10; //每个用户去过10个以上的餐厅
        int itemLeastCount = 10; //每家餐厅被10个以上的用户访问过
//        int dishLeastCount = 10; // 每家餐厅至少有10道菜
//        int dishReviewedLeastCount = 10;//每道菜至少被10个不同的<user-shop>对访问过
        boolean isMapping = true; //保存的签到记录中id是否映射为顺序id, true为映射，false为保留原来的id

        File desFile = new File(desPath);//新建输出文件
        if (!desFile.exists()) {
            desFile.mkdir();
        }

        System.out.println("shopInfoPath: " + shopInfoPath);
        System.out.println("reviewPath: " + reviewPath);
        System.out.println("despath: " + desPath);
        System.out.println("filter condition " + userLeastCount + " " + itemLeastCount );

        /**
         * 获取原始文件数据 shop信息和checkin记录
         */

        boolean filterByCategory = false;
        shopCategoryMap = new HashMap<>();
//        getShopInfo(shopInfoPath, , filterByCategory);
        getCheckInRecord(reviewPath);

        /**
         *对数据进行过滤，
         */
        Map<String, Set<String>> userItemSetMap = getUserItemSetMapofRecord();
//        Map<String, Map<String, Integer>> shopDishSetMap = getShopDishSetMapofRecord();
//        Map<String, Map<String, Set<String>>> dishPostSetMap = getDishPostSetMap();

        int iteration = -1;//表示会不断迭代直到同时满足least限制条件
//        Set<String>[] resultSet = CircularFilter.shopCountFilter(checkInRecordMap, userItemSetMap,  userLeastCount, itemLeastCount, iteration);
//   //     globalshopDishSetMap = CircularFilter.get_shopDishSetMap();
        globalUserItemSetMap = CircularFilter.get_userItemSetMap();
//   //     globalDishPostSetMap = CircularFilter.get_dishPostSetMap();

//        outputResultWithMap(globalUserItemSetMap, resultSet, desPath, userLeastCount, itemLeastCount, isMapping);
        System.out.println("end " + new Date());
    }
    /**
     * user-shop-category-time
     *统计<userid-shopid-dishid>三元组的个数？
     * 无需解析review.json
     * @param reviewPath
     */
    public static void getCheckInRecord(String reviewPath) {
        System.out.println("checkinrecord解析开始checkinWithTimestamp.txt");
        String read;
        FileInputStream file;
        BufferedReader bufferedReader;
        checkInRecordMap = new HashMap<>();
        originUserSet = new HashSet<>();
        originShopSet = new HashSet<>();
        userSet = new HashSet<>();
        shopSet = new HashSet<>();
        //记录重复的<user,shop,dish>三元组的个数
/*        int duplicateTriplePairNum = 0;
        Map<String, Set<String>> userDishPair = new HashMap<>();
        Map<String, Set<String>> shopDishPair = new HashMap<>();*/

        try {
            file = new FileInputStream(reviewPath);
            bufferedReader = new BufferedReader(new InputStreamReader(file, "UTF-8"));
            while ((read = bufferedReader.readLine()) != null) {
//                originReviewCount++;
                String[] contents = read.trim().split("[ \t,]+");
//                String record = read;
                String userId = contents[0];
                String shopId = contents[1];
//                String categoryId = contents[2];
//                String time = contents[3];
                originUserSet.add(userId);
                originShopSet.add(shopId);
//                if (shopInfoIdSet.contains(shopId) && StringUtils.isNumeric(userId) && StringUtils.isNumeric(shopId) ) {
                    reviewCount++;
                    if (!checkInRecordMap.containsKey(userId))//将新的userid加入
                        checkInRecordMap.put(userId, new HashMap<>());

                    if (!checkInRecordMap.get(userId).containsKey(shopId)) {//将新的shopid加入
                        checkInRecordMap.get(userId).put(shopId, 1);
                        shopSet.add(shopId);
                    }else {
                        checkInRecordMap.get(userId).put(shopId,checkInRecordMap.get(userId).get(shopId)+ 1);
//                      checkInRecordMap.get(userId).get(shopId).put(checkInRecordMap.get(userId).get(shopId)+ 1);
                    }



//                }
            }bufferedReader.close();

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 签到次数map
     * @return
     */
    public static Map<String, Set<String>> getUserItemSetMapofRecord() {
        int userShopPairNum = 0;
        Map<String, Set<String>> userItemSetMap = new HashMap<>();
        System.out.println("review中user-shop Pair的数量 " + userShopPairNum);
        return userItemSetMap;
    }

    /**
     *
     * @param shopInfoPath
     * @param categoryFilterPath
     * @param filterByCategory
     */
    public static void getShopInfo(String shopInfoPath, String categoryFilterPath, boolean filterByCategory){

    }

    /**
     * //做映射用的map
     * @param userItemSetMap
     * @param resultSet
     * @param desPath
     * @param userLeastCount
     * @param itemLeastCount
     * @param isMapping
     */
    public static void outputResultWithMap(Map<String, Set<String>> userItemSetMap,
                                           Set<String>[] resultSet, String desPath, int userLeastCount, int itemLeastCount, boolean isMapping) {

    }

}
