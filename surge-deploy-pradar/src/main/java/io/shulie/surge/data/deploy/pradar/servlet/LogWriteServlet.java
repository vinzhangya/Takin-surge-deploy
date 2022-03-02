package io.shulie.surge.data.deploy.pradar.servlet;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.pamirs.pradar.remoting.protocol.CommandCode;
import io.shulie.surge.data.deploy.pradar.common.ResponseCodeEnum;
import io.shulie.surge.data.deploy.pradar.model.ResponseDataModel;
import io.shulie.surge.data.runtime.disruptor.RingBufferIllegalStateException;
import io.shulie.surge.data.runtime.processor.DataQueue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author Sunsy
 * @date 2022/2/24
 * @apiNode
 * @email sunshiyu@shulie.io
 */
@Singleton
public class LogWriteServlet extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(LogWriteServlet.class);

    protected Map<String, DataQueue> queueMap;

    public Map<String, DataQueue> getQueueMap() {
        return queueMap;
    }

    public void setQueueMap(Map<String, DataQueue> queueMap) {
        this.queueMap = queueMap;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doHead(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("receive log =======================");
        long now = System.currentTimeMillis();
        ResponseDataModel responseDataModel = new ResponseDataModel(now, String.valueOf(CommandCode.SUCCESS), ResponseCodeEnum.CODE_0000.getMsg());

        try {
            String content = IOUtils.toString(request.getInputStream());
            if (StringUtils.isNotBlank(content)) {
                String hostIp = request.getHeader("hostIp");
                String dataVersion = request.getHeader("version");
                String dataType = request.getHeader("dataType");
                if (StringUtils.isBlank(hostIp) || StringUtils.isBlank(dataVersion) || StringUtils.isBlank(dataType)) {
                    responseDataModel.setResponseMsg("缺少header:hostIp or version or dataType");
                    responseDataModel.setResponseCode(String.valueOf(CommandCode.COMMAND_CODE_NOT_SUPPORTED));
                } else {
                    byte dateTypeByte = 0;
                    switch (dataType) {
                        case "1":
                            dateTypeByte = 1;
                            break;
                        case "2":
                            dateTypeByte = 2;
                            break;
                        case "3":
                            dateTypeByte = 3;
                            break;
                        case "4":
                            dateTypeByte = 4;
                            break;
                        default:
                            break;
                    }
                    DataQueue queue = queueMap.get(dataType);
                    Map<String, Object> header = Maps.newHashMap();
                    header.put("hostIp", hostIp);
                    header.put("dataVersion", dataVersion);
                    header.put("dataType", dateTypeByte);
                    queue.publish(header, queue.splitLog(content, dateTypeByte));
                }
            } else {
                responseDataModel.setResponseMsg("日志内容为空");
                responseDataModel.setResponseCode(String.valueOf(CommandCode.COMMAND_CODE_NOT_SUPPORTED));
            }

        } catch (RingBufferIllegalStateException e) {
            logger.error(e.getMessage());
            responseDataModel.setResponseMsg("系统繁忙");
            responseDataModel.setResponseCode(String.valueOf(CommandCode.SYSTEM_BUSY));
        } catch (Throwable e) {
            logger.error("logProcessor fail " + ExceptionUtils.getStackTrace(e));
            responseDataModel.setResponseMsg("处理异常");
            responseDataModel.setResponseCode(String.valueOf(CommandCode.SYSTEM_ERROR));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().println(JSONObject.toJSONString(responseDataModel));

    }


    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }
}
