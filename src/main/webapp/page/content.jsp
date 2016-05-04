<%@ page session="true" contentType="text/html" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<c:if test="${empty hasInit}">
    <pg:init>
        <c:if test="${empty hasVars}">
            <pg:jsvars/>
        </c:if>
    </pg:init>
</c:if>

<div id="page-wrapper">
    <div class="pad">
        <div id="page-content">loading...</div>
    </div>
</div>

<script>
    (function(){

        var customPage = XNAT.app.customPage;
        var $pageContent = $('#page-content');

        customPage.getPage('', $pageContent);

//        window.onhashchange = function(){
//            customPage.getPage('', $pageContent);
//        }

    })();
</script>