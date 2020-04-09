package com.bitcoin.indexer.controllers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeStacktrace;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Controller
public class CustomErrorController implements ErrorController {

	private final ErrorProperties errorProperties = new ErrorProperties();
	private final ErrorAttributes errorAttributes = new DefaultErrorAttributes(true);
	private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

	@RequestMapping("/error")
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
		HttpStatus status = this.getStatus(request);
		if (status == HttpStatus.NO_CONTENT) {
			logger.error("Error thrown returning only status={}", status);
			return new ResponseEntity(status);
		} else {
			Map<String, Object> body = this.getErrorAttributes(request, this.isIncludeStackTrace(request, MediaType.ALL));
			ResponseEntity responseEntity = new ResponseEntity(body, status);
			logger.error("Error thrown returning={}", body.entrySet());
			return responseEntity;
		}
	}

	@ExceptionHandler({ HttpMediaTypeNotAcceptableException.class })
	public ResponseEntity<String> mediaTypeNotAcceptable(HttpServletRequest request) {
		HttpStatus status = this.getStatus(request);
		return ResponseEntity.status(status).build();
	}

	protected boolean isIncludeStackTrace(HttpServletRequest request, MediaType produces) {
		IncludeStacktrace include = this.getErrorProperties().getIncludeStacktrace();
		if (include == IncludeStacktrace.ALWAYS) {
			return true;
		} else {
			return include == IncludeStacktrace.ON_TRACE_PARAM && this.getTraceParameter(request);
		}
	}

	protected ErrorProperties getErrorProperties() {
		return this.errorProperties;
	}

	public HttpStatus getStatus(HttpServletRequest request) {
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		if (statusCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		} else {
			try {
				return HttpStatus.valueOf(statusCode);
			} catch (Exception var4) {
				return HttpStatus.INTERNAL_SERVER_ERROR;
			}
		}
	}

	@Override
	public String getErrorPath() {
		return "error";
	}

	protected Map<String, Object> getErrorAttributes(HttpServletRequest request, boolean includeStackTrace) {
		WebRequest webRequest = new ServletWebRequest(request);
		return this.errorAttributes.getErrorAttributes(webRequest, includeStackTrace);
	}

	protected boolean getTraceParameter(HttpServletRequest request) {
		String parameter = request.getParameter("trace");
		if (parameter == null) {
			return false;
		} else {
			return !"false".equalsIgnoreCase(parameter);
		}
	}
}
