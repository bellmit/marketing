package com.jgw.supercodeplatform.marketing.service.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jgw.supercodeplatform.marketing.common.model.es.EsSearch;
import com.jgw.supercodeplatform.marketing.common.util.CommonUtil;
import com.jgw.supercodeplatform.marketing.common.util.SpringContextUtil;
import com.jgw.supercodeplatform.marketing.exception.CodePlatformException;
import com.jgw.supercodeplatform.marketing.exception.EsException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * es??????????????????
 *
 * @author Created by jgw136 on 2018/04/26.
 */
public abstract class AbstractEsSearch extends CommonUtil {

    /**
     * ???????????????????????????
     */
    protected static final String AggregationName="agg";
    protected static final SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");

    /**
     * @param esSearch ??????????????????
     * @return Object
     * @throws CodePlatformException
     */
    public List<SearchHit> get(EsSearch esSearch){


        TransportClient eClient = SpringContextUtil.getBean("elClient");

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (esSearch.getParam() != null && esSearch.getParam().size() > 0) {
            esSearch.getParam().forEach((k, v) -> boolQueryBuilder.must(QueryBuilders.termQuery(k, v)));
        }

        if (esSearch.getQueryBuilders() != null && esSearch.getQueryBuilders().size() > 0){
            esSearch.getQueryBuilders().forEach(queryBuilder -> boolQueryBuilder.must(queryBuilder));
        }

        SearchRequestBuilder requestBuilder = eClient
                .prepareSearch(esSearch.getIndex().getIndex())
                .setTypes(esSearch.getType().getType())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolQueryBuilder);

        int size = esSearch.getPage().getSize();

        requestBuilder.setSize(size == 0 ? 100 : size);

        if (esSearch.getSortBuilder() != null) {
            requestBuilder.addSort(esSearch.getSortBuilder());
        }

        SearchResponse response = requestBuilder.setExplain(true).execute().actionGet();

        SearchHit[] hits = response.getHits().getHits();

        return Arrays.asList(hits);
    }

    /**
     * @param esSearch ??????????????????
     * @return Object
     * @throws CodePlatformException
     */
    public List<SearchHit> getByScroll(EsSearch esSearch){


        TransportClient eClient = SpringContextUtil.getBean("elClient");

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        esSearch.getParam().forEach((k, v) -> boolQueryBuilder.must(QueryBuilders.termQuery(k, v)));


        SearchRequestBuilder requestBuilder = eClient
                .prepareSearch(esSearch.getIndex().getIndex())
                .setTypes(esSearch.getType().getType())
                .setScroll(TimeValue.timeValueMinutes(1))
                .setQuery(boolQueryBuilder);

        int size = esSearch.getPage().getSize();

        requestBuilder.setSize(size == 0 ? 100 : size);

        if (esSearch.getSortBuilder() != null) {
            requestBuilder.addSort(esSearch.getSortBuilder());
        }

        SearchResponse response = requestBuilder.setExplain(true).execute().actionGet();

        List<SearchHit> searchHitList = new ArrayList<>(Integer.valueOf(String.valueOf(response.getHits().getTotalHits())));

        do {

            SearchHit[] hits = response.getHits().getHits();
            searchHitList.addAll(Arrays.asList(hits));
            response = eClient.
                    prepareSearchScroll(response.getScrollId()).setScroll(TimeValue.timeValueMinutes(1)).execute().actionGet();

        } while (response.getHits().getHits().length != 0);

        return searchHitList;
    }

    /**
     * ??????es?????????map????????????????????????id
     *
     * @param searchHitList
     * @return
     * @throws Exception
     */
    public List<Map<String, Object>> get(List<SearchHit> searchHitList) {
        List<Map<String, Object>> mapList = new ArrayList<>(searchHitList.size());
        Map<String, Object> map = null;
        for (SearchHit searchHit : searchHitList) {
            map = searchHit.getSourceAsMap();
            map.put("id", searchHit.getId());
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * ??????es????????????????????????????????????????????????id???????????????????????????id
     *
     * @param searchHitList
     * @param transferClass
     * @return
     * @throws Exception
     */
    public <T> T get(List<SearchHit> searchHitList, Class<?> transferClass)  {
        List<Object> objectList = new ArrayList<>(searchHitList.size());
        Object o = null;
        Field field = null;
        for (SearchHit searchHit : searchHitList) {
            o = JSONObject
                    .parseObject(JSON.toJSONString(searchHit.getSourceAsMap()), transferClass);
            try {
                field = o.getClass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(o, searchHit.getId());
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            objectList.add(o);
        }
        return (T) objectList;
    }

    /**
     * ???????????????????????????count???
     *
     * @param esSearch
     * @return
     */
    public long getCount(EsSearch esSearch) {
        TransportClient eClient = SpringContextUtil.getBean("elClient");

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        esSearch.getParam().forEach((k, v) -> boolQueryBuilder.must(QueryBuilders.termQuery(k, v)));
        SearchRequestBuilder requestBuilder = eClient
                .prepareSearch(esSearch.getIndex().getIndex())
                .setTypes(esSearch.getType().getType())
                .setScroll(TimeValue.timeValueMinutes(1))
                .setQuery(boolQueryBuilder);
        ValueCountAggregationBuilder aggregation = AggregationBuilders.count(AggregationName).field("_id");
        requestBuilder.addAggregation(aggregation);
        // ??????????????????
        SearchResponse searchResponse = requestBuilder.execute().actionGet();
        // ??????count
        ValueCount aggs = searchResponse.getAggregations().get(AggregationName);
        // ????????????????????????????????????????????????
        // ??????????????????
        // ??????????????????????????????????????????
        // ???????????????
        return aggs.getValue();
    }

    /**
     * ????????????
     *
     * @param esSearch
     * @param updateParams
     * @throws EsException
     */
    public int update(EsSearch esSearch, Map<String, Object> updateParams) throws EsException {

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        esSearch.getParam().forEach((k, v) -> boolQueryBuilder.must(QueryBuilders.termQuery(k, v)));


        SearchResponse response = eClient
                .prepareSearch(esSearch.getIndex().getIndex())
                .setTypes(esSearch.getType().getType())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                //????????????
                //.setQuery(QueryBuilders.matchAllQuery())
                .setQuery(boolQueryBuilder).get();
        SearchHit[] hits = response.getHits().getHits();
        if (hits.length <= 0) {
            throw new EsException("?????????????????????????????????????????????");
        }
        String id = hits[0].getId();
        return update(esSearch, id, updateParams);
    }

    /**
     * @param esSearch
     * @param id
     * @param updateParams
     * @throws EsException
     */
    public int update(EsSearch esSearch, String id, Map<String, Object> updateParams) throws EsException {

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(esSearch.getIndex().getIndex());
        updateRequest.type(esSearch.getType().getType());
        updateRequest.id(id);
        try {
            XContentBuilder xContentBuilder = jsonBuilder().startObject();
            updateParams.forEach(((name, value) -> {
                try {
                    xContentBuilder.field(name, value);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("update?????????????????????");
                }
            }));
            xContentBuilder.endObject();
            updateRequest.doc(xContentBuilder);
        } catch (IOException e) {
            e.printStackTrace();
            throw new EsException("update:??????????????????");
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new EsException("update:??????????????????");
        }
        try {
            UpdateResponse actionFuture = eClient.update(updateRequest).get();
            return actionFuture.status().getStatus();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new EsException("update:??????????????????");
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new EsException("update:??????????????????");
        }
    }

    public void add(EsSearch esSearch, Map<String, Object> addParam) {
    	add(esSearch, false,  addParam);
    }
    
    /**
     * ????????????
     * @param esSearch 
     * @param immediateFlag ???????????????????????????????????????????????????ES???
     * @param addParam
     */
    public void add(EsSearch esSearch, boolean immediateFlag, Map<String, Object> addParam) {

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        IndexRequestBuilder indexRequest = eClient
                .prepareIndex(esSearch.getIndex().getIndex(), esSearch.getType().getType())
                .setSource(addParam);
        if(immediateFlag) {
        	indexRequest.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        }
        indexRequest.get();
    }

    /**
     * ???????????????????????????es??????
     *
     * @param esSearch
     * @throws EsException
     */
    public long delete(EsSearch esSearch) {

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        esSearch.getParam().forEach((k, v) -> queryBuilder.must(QueryBuilders.termQuery(k, v)));

        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(eClient)
                .filter(queryBuilder)
                .source(esSearch.getIndex().getIndex())
                .get();
        return response.getDeleted();
    }

    /**
     * ??????id??????
     *
     * @param esSearch
     * @param id
     */
    public int deleteById(EsSearch esSearch, String id) {

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        DeleteResponse dResponse = eClient
                .prepareDelete(
                        esSearch.getIndex().getIndex()
                        , esSearch.getType().getType()
                        , id)
                .execute().actionGet();
        return dResponse.status().getStatus();
    }

    /**
     * ???????????????????????? ??????????????????????????????
     *
     * @param indexName
     * @return
     */
    public static boolean isIndexExists(String indexName) {
        boolean flag = false;

        TransportClient eClient = SpringContextUtil.getBean("elClient");

        IndicesExistsRequest inExistsRequest = new IndicesExistsRequest(indexName);

        IndicesExistsResponse inExistsResponse = eClient.admin().indices()
                .exists(inExistsRequest).actionGet();

        if (inExistsResponse.isExists()) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    /**
     * ???????????????
     *
     * @param indexName
     */
    public static void deleteIndex(String indexName) {

        if (!isIndexExists(indexName)) {
            System.out.println(indexName + " not exists");
        } else {
            TransportClient eClient = SpringContextUtil.getBean("elClient");
            DeleteIndexResponse dResponse = eClient.admin().indices().prepareDelete(indexName)
                    .execute().actionGet();
            if (dResponse.isAcknowledged()) {
                System.out.println("delete index " + indexName + "  successfully!");
            } else {
                System.out.println("Fail to delete index " + indexName);
            }
        }
    }
}
