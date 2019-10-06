<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<script src="http://libs.baidu.com/jquery/1.9.0/jquery.js" type="text/javascript"></script>
<html>
<%
    String path = request.getRequestURI();
    String basePath = request.getScheme() + "://"
            + request.getServerName() + ":" + request.getServerPort()
            + path;
%>
<base href="<%=basePath%>">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>JSP Page</title>
    <style>
        #jz {
            margin: 0 auto;
            width: 500px;
            height: 200px;
            /*border: 1px solid #F00*/
        }
    </style>
</head>
<body>
<div id="jz">
    <table id="table" <%--style="border:1px solid #F00"--%>>
        <tr>
            <td>
                请选择抓取规则:
                <label><input type="radio" name="guiZe" value="1" checked="checked">规则1</label>
                <label><input type="radio" name="guiZe" value="2">规则2</label>
                <br />
            </td>
        </tr>
        <tr>
            <td>
                <input id="miyao" placeholder="请输入密码"/>
                <button id="begin" type="button">抓取数据</button>
            </td>
        </tr>
        <tr id="noClose" style="display: none">
            <td>
                正在抓取数据，请勿刷新或关闭浏览器!
            </td>
        </tr>
        <tr id="noRepetition" style="display: none">
            <td>
                已有程序运行，请勿重复操作！
            </td>
        </tr>
        <tr id="trGameCount" style="display: none">
            <td>
                <span id="nowTime"></span> 共 <span id="gameCount"></span> 场比赛。
            </td>
        </tr>
        <tr id="trAlreadyCount" style="display: none">
            <td>
                已抓取 <span id="alreadyCount" style="color: #FF0000">0</span> 场比赛。
            </td>
        </tr>
        <tr id="downLoad" style="display: none">
            <td id="downLoadFile">
                <%--数据提取完毕，<a href="http://localhost:8080/zhua/zhua/download">点击下载</a>--%>
            </td>
        </tr>
    </table>
</div>
</body>
<script type="text/javascript">
    $(function () {
        //var warName = "/zhua";
         var warName = "";
        // 浏览器准备好了，就加载需要加载的请求
        // 比赛总数量轮询器
        var gameCountTime = null;
        // 已抓取比赛数量轮询器
        var alreadyCountTime = null;
        // 轮询器处罚时间
        var times = 1000;

        nowTime();

        //找到XX元素，一般给元素加id
        var begin = document.getElementById('begin');
        //给xx元素加事件
        begin.onclick = function () {
            var guiZe = $("input[name='guiZe']:checked").val();
            //代码段
            var str = document.getElementById('miyao').value;
            //alert(str);
            // 判断输入的密码是否正确
            $.ajax({
                type: 'get',
                url: warName + "/zhua/checkPassword",
                data: {"password": str},// 请求数据，json格式
                dataType: 'text', //dataType 后台返回的数据类型
                success: function success(data) {
                    if (data == 0) {
                        if (confirm("密码验证成功，点击确定开始抓取数据！")) {
                            // 禁用抓取数据按钮
                            document.getElementById("begin").setAttribute("disabled", true);
                            // 禁用单选按钮
                            $("input[type=radio]").each(function(){
                                $(this).attr("disabled",true);
                            });
                            document.getElementById("noClose").style.display = "block";
                            // 1、开启比赛总数量轮询
                            gameCountTime = setInterval(getGameCount, times);
                            // 调用抓取数据
                            downLoadData(guiZe);
                        }
                    } else if (data == 2) {
                        // 这里逻辑没用
                        alert("已无抓取次数，请联系管理员！");
                    } else {
                        alert("密码错误！");
                    }
                },
                error: function error() {
                    alert('系统异常，请联系管理员！');
                }
            });
        }

        function downLoadData(guiZe) {
            $.ajax({
                type: 'get',
                url: warName + "/zhua/zhuaDataAndCreateFile",
                data: {"guiZe": guiZe},// 请求数据，json格式
                dataType: 'text', //dataType 后台返回的数据类型
                success: function success(data) {
                    if (data == 1) {
                        clearInterval(gameCountTime);
                        clearInterval(alreadyCountTime);
                        document.getElementById("noRepetition").style.display = "block";
                        ;
                        alert("正在抓取数据，请勿重复操作！");
                    } else if (data == 2) {
                        clearInterval(gameCountTime);
                        clearInterval(alreadyCountTime);
                        alert("系统异常，请联系管理员！");
                    }
                },
                error: function error() {
                    alert("抓取数据异常，请联系管理员！")
                }
            });
        }

        // 获取比赛总场次函数
        function getGameCount() {
            // console.log("比赛总场次");
            $.ajax({
                type: 'get',
                url: warName + "/zhua/getGameCount",
                //data: ,// 请求数据，json格式
                dataType: 'text', //dataType 后台返回的数据类型
                success: function success(data) {
                    if (data != null && data > 0) {
                        // console.log("======================比赛总场次");
                        document.getElementById("trGameCount").style.display = "block";
                        document.getElementById('gameCount').innerText = data;
                        // 2、查询到数据，并且数量大于0，清除比赛总数量轮询
                        clearInterval(gameCountTime);
                        // 3、开启已抓取比赛数量轮询 1000毫秒 查一次
                        alreadyCountTime = setInterval(getAlreadyCount, times);

                    }
                },
                error: function error() {
                    alert('获取比赛总场次，系统异常，请联系管理员！');
                }
            });
        }

        // 已抓取比赛总数量函数
        function getAlreadyCount() {
            //console.log("已抓取比赛数量");
            // 显示已抓取行
            document.getElementById("trAlreadyCount").style.display = "block";
            $.ajax({
                type: 'get',
                url: warName + "/zhua/getAlreadyCount",
                //data: ,// 请求数据，json格式
                dataType: 'text', //dataType 后台返回的数据类型
                success: function success(data) {
                    // 比赛总数量
                    var gameCount = document.getElementById('gameCount').innerHTML;
                    if (data == gameCount) {
                        // console.log("======================已抓取比赛数量");
                        // 显示正在下载数据
                        document.getElementById("downLoad").style.display = "block";
                        document.getElementById("downLoadFile").innerHTML = "数据提取完毕，<a href=\"http://localhost:8080/"+warName+"/zhua/download\">点击下载</a>";


                        // 4、已抓取全部比赛，清除轮询器
                        clearInterval(alreadyCountTime);
                    }
                    if (data != null && data > 0) {
                        document.getElementById('alreadyCount').innerText = data;
                    }
                },
                error: function error() {
                    alert('获取已抓取比赛场次，系统异常，请联系管理员！');
                }
            });
        }

        // 获取当前日期
        function nowTime() {
            //获取年月日
            var time = new Date();
            var year = time.getFullYear();
            var month = time.getMonth() + 1;
            var day = time.getDate();
            document.getElementById("nowTime").innerText = year + "年" + month + "月" + day + "日，";
        }
    });
</script>
</html>