package com.mobilepark.airtalk.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mobilepark.airtalk.data.Alarm;
import com.mobilepark.airtalk.repository.AlarmRepository;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AlarmService {

  private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

  @Autowired
  public AlarmRepository alarmRepository;
  
  @Autowired
  SpecificationService<Alarm> specificationService;

  private Specification<Alarm> getSpecification(String type, String search) {
    
    Alarm alarm = new Alarm();
    Set<String> likeSet = new HashSet<>();

    if(StringUtils.isNotEmpty(search) && StringUtils.equals(type, "userId"))
      alarm.setUserId(search);
    else if(StringUtils.isNotEmpty(search) && StringUtils.equals(type, "code"))
      alarm.setCode(search);     

    likeSet.add(type);
    return specificationService.like(likeSet, alarm);
  }


  /**
   * 알림 게시판 검색 조회
   * 
   * @return List<Alarm>
   */
  public Map<String, Object> list(JSONObject form) {

    List<Alarm> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    String type = "";
    String keyword = "";
    int start = 0;
    int length = 0;
    
    Iterator<?> keys = form.keySet().iterator();
    while(keys.hasNext()) {
      String key = keys.next().toString();
      if(key.equals("user_id")) 
      {
        type = "user_id";
        keyword = form.get(key).toString();
      }
      else if(key.equals("code"))
      {
        type = "code";
        keyword = form.get(key).toString();
      } 
      else if(key.equals("reserv_date")) 
      {
        type = "reserv_date";
        keyword = form.get(key).toString();
      } 
      else if(key.equals("start")) start = Integer.parseInt(form.get(key).toString());
      else if(key.equals("length")) length = Integer.parseInt(form.get(key).toString());
      else {
        map.put("err_cd", "-11000");
        return map;
      }
    }

    logger.info("params : [type : "+type+"][keyword : "+keyword+"][start : "+start+"][length : "+length+"]");

    if(length != 0) {
      PageRequest pageRequest = PageRequest.of(start, length);

      try {
        list = alarmRepository.findAll(this.getSpecification(type, keyword), pageRequest).getContent();
        map.put("result", list);
        map.put("err_cd", "0000");
        map.put("total_cnt", list.size());
      } catch (Exception e) {
        map.put("err_cd", "-1000");
        e.printStackTrace();
      }

    } else {
      
      try {
        if(type.equals("user_id")) list = alarmRepository.findByUserId(keyword);
        if(type.equals("code")) list = alarmRepository.findByCode(keyword);
        // if(type.equals("reserv_date")) list = alarmRepository.findeByReservDate(keyword);
        map.put("result", list);
        map.put("err_cd", "0000");
        map.put("total_cnt", list.size());
      } catch (Exception e) {
        map.put("err_cd", "-1000");
        e.printStackTrace();
      }
    } 

    return map;
  }

  /**
   * 알림 등록
   * 
   * @return Map<String, String>
   */
  public Map<String, String> create(JSONObject form) {


    Map<String, String> result = new HashMap<>();
    Alarm alarm = this.getParameter(form, "create");
    alarm.setRegDate(new Date());
    logger.info("params : [userId : "+alarm.getUserId()+"][message : "+alarm.getMessage()+"][code : "+alarm.getCode()+"]"+
                         "[latitude : "+alarm.getLatitude()+"][longitude : "+alarm.getLongitude()+"[bdNm : "+alarm.getBdNm()+"]"+
                         "[reservDate : "+alarm.getReservDate()+"]");

    try {
      alarmRepository.save(alarm);
      result.put("err_cd", "0000");
    } catch (Exception e) {
      result.put("err_cd", "-1000");
      e.getStackTrace();
    }

    return result;
  }

  /**
   * 알림 수정
   * 
   * @return Map<String, String>
   */
  public Map<String, String> modify(JSONObject form) {

    Alarm alarm = this.getParameter(form, "modify");
    Map<String, String> result = new HashMap<>();

    logger.info("params : [seq : "+alarm.getSeq()+"][message : "+alarm.getMessage()+"][reservDate : "+alarm.getReservDate()+"]");


      alarmRepository.save(alarm);
      result.put("err_cd", "0000");


    return result;
  }

  /**
   * 알림 삭제
   * 
   * @return Map<String, String>
   */
  public Map<String, String> remove(JSONObject form) {

    Alarm alarm = this.getParameter(form, "remove");
    Map<String, String> result = new HashMap<>();

    logger.info("params : [seq : "+alarm.getSeq()+"]");

    try {
      alarmRepository.deleteById(alarm.getSeq());
      result.put("err_cd", "0000");
    } catch (Exception e) {
      result.put("err_cd", "-1000");
      e.getStackTrace();
    }

    return result;
  }

  /**
   * 알림 게시판 검색 카운트 조회
   * 
   * @return Integer
   */
  public int count(JSONObject form) {
    int count = 0;

    if(!form.get("user_id").toString().isEmpty()) {
      count = alarmRepository.countByUserIdContaining(form.get("user_id").toString());
    } else if(!form.get("code").toString().isEmpty()) {
      count = alarmRepository.countByCodeContaining(form.get("code").toString());
    } 
    // else if(!form.get("reserv_date").toString().isEmpty()) {
      // try {
      //   count = alarmRepository.countByReservDateContaining(form.get("keyword").toString());
      // }catch (Exception e) {
      //   e.printStackTrace();
      // }
    // }

    logger.info("Total Count : ["+count+"]");

    return count;
  }

  /**
   * 알림 서비스 파라미터
   * 
   * @return Alarm
   */
  public Alarm getParameter(JSONObject form, String service) {
    
    Alarm alarm = new Alarm();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddmmss");

    if(service.equals("modify") || service.equals("remove")) {
      System.out.println("modify + remove = seq");
      alarm.setSeq(Integer.parseInt(form.get("seq").toString()));
    }
    
    if(service.equals("modify") || service.equals("create")) {
      System.out.println("modify + create = message, reservDate");
      alarm.setMessage(form.get("message").toString());
      try{
      alarm.setReservDate(sdf.parse(form.get("reserv_date").toString()));
      }catch (Exception e){
        e.printStackTrace();
      }
    } 
    
    if(service.equals("create")) {
      System.out.println("create = userId, code, latitude, loginitudem, bdNm");
      alarm.setUserId(form.get("user_id").toString());
      alarm.setCode(form.get("code").toString());
      alarm.setLatitude(new BigDecimal(form.get("latitude").toString()));
      alarm.setLongitude(new BigDecimal(form.get("longitude").toString()));
      alarm.setBdNm(form.get("bd_nm").toString());
    }
    return alarm;
  }

}
