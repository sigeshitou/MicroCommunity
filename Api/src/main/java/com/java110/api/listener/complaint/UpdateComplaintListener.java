package com.java110.api.listener.complaint;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.api.listener.AbstractServiceApiListener;
import com.java110.core.smo.complaint.IComplaintInnerServiceSMO;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ServiceCodeConstant;
import com.java110.utils.util.Assert;
import com.java110.core.annotation.Java110Listener;
import com.java110.core.context.DataFlowContext;
import com.java110.entity.center.AppService;
import com.java110.event.service.api.ServiceDataFlowEvent;
import com.java110.utils.constant.ServiceCodeComplaintConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 保存投诉建议侦听
 * add by wuxw 2019-06-30
 */
@Java110Listener("updateComplaintListener")
public class UpdateComplaintListener extends AbstractServiceApiListener {

    @Autowired
    private IComplaintInnerServiceSMO complaintInnerServiceSMOImpl;

    @Override
    protected void validate(ServiceDataFlowEvent event, JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "complaintId", "投诉ID不能为空");
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
        Assert.hasKeyAndValue(reqJson, "typeCd", "必填，请选择投诉类型");
        //Assert.hasKeyAndValue(reqJson, "roomId", "必填，请选择房屋编号");
        Assert.hasKeyAndValue(reqJson, "complaintName", "必填，请填写投诉人");
        Assert.hasKeyAndValue(reqJson, "tel", "必填，请填写投诉电话");
        //Assert.hasKeyAndValue(reqJson, "state", "必填，请填写投诉状态");
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写投诉内容");

    }

    @Override
    protected void doSoService(ServiceDataFlowEvent event, DataFlowContext context, JSONObject reqJson) {

        HttpHeaders header = new HttpHeaders();
        context.getRequestCurrentHeaders().put(CommonConstant.HTTP_ORDER_TYPE_CD, "D");
        JSONArray businesses = new JSONArray();

        AppService service = event.getAppService();

        //添加单元信息
        businesses.add(updateComplaint(reqJson, context));

        JSONObject paramInObj = super.restToCenterProtocol(businesses, context.getRequestCurrentHeaders());

        //将 rest header 信息传递到下层服务中去
        super.freshHttpHeader(header, context.getRequestCurrentHeaders());

        ResponseEntity<String> responseEntity = this.callService(context, service.getServiceCode(), paramInObj);

        context.setResponseEntity(responseEntity);
    }

    @Override
    public String getServiceCode() {
        return ServiceCodeComplaintConstant.UPDATE_COMPLAINT;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }


    /**
     * 添加投诉建议信息
     *
     * @param paramInJson     接口调用放传入入参
     * @param dataFlowContext 数据上下文
     * @return 订单服务能够接受的报文
     */
    private JSONObject updateComplaint(JSONObject paramInJson, DataFlowContext dataFlowContext) {

        ComplaintDto complaintDto = new ComplaintDto();
        complaintDto.setStoreId(paramInJson.getString("storeId"));
        complaintDto.setComplaintId(paramInJson.getString("complaintId"));
        List<ComplaintDto> complaintDtos = complaintInnerServiceSMOImpl.queryComplaints(complaintDto);

        Assert.listOnlyOne(complaintDtos, "存在多条记录，或不存在数据" + complaintDto.getComplaintId());


        JSONObject business = JSONObject.parseObject("{\"datas\":{}}");
        business.put(CommonConstant.HTTP_BUSINESS_TYPE_CD, BusinessTypeConstant.BUSINESS_TYPE_UPDATE_COMPLAINT);
        business.put(CommonConstant.HTTP_SEQ, DEFAULT_SEQ);
        business.put(CommonConstant.HTTP_INVOKE_MODEL, CommonConstant.HTTP_INVOKE_MODEL_S);
        JSONObject businessComplaint = new JSONObject();
        businessComplaint.putAll(paramInJson);
        businessComplaint.put("state", complaintDtos.get(0).getState());
        businessComplaint.put("roomId", complaintDtos.get(0).getRoomId());
        //计算 应收金额
        business.getJSONObject(CommonConstant.HTTP_BUSINESS_DATAS).put("businessComplaint", businessComplaint);
        return business;
    }

    public IComplaintInnerServiceSMO getComplaintInnerServiceSMOImpl() {
        return complaintInnerServiceSMOImpl;
    }

    public void setComplaintInnerServiceSMOImpl(IComplaintInnerServiceSMO complaintInnerServiceSMOImpl) {
        this.complaintInnerServiceSMOImpl = complaintInnerServiceSMOImpl;
    }
}
