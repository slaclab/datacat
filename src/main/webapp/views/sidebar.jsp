<%-- 
    Document   : sidebar
    Created on : Aug 20, 2015, 1:45:32 PM
    Author     : bvan
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<div class="sidebar-wrapper nicescroll" style="overflow: hidden; outline: none;" tabindex="0">
    <ul class="nav nav-sidebar">

        <li class="home ${view == 'browser' ? 'active' : ''}">
            <a data-placement="right" href="${pageContext.request.contextPath}/display/browser${target.path}" title="Browser">
                <span class="glyphicon glyphicon-th-list" aria-hidden="true"></span>
                <span class="nav-word">Browser</span>
            </a>
        </li>
        <li class="${view == 'tree' ? 'active' : ''}">
            <a data-placement="right" href="${pageContext.request.contextPath}/display/tree${target.path}" title="Tree">
                <span class="glyphicon glyphicon-tree-deciduous" aria-hidden="true"></span>
                <span class="nav-word">Tree</span>
            </a>
        </li>
        <li class="${view == 'datasets' ? 'active' : ''}">
            <a data-placement="right" href="${pageContext.request.contextPath}/display/datasets${target.path}" title="Datasets">
                <span class="glyphicon glyphicon-duplicate" aria-hidden="true"></span>
                <span class="nav-word">Datasets</span>
            </a>
        </li>
    </ul>

    <div class="collapse-nav">
        <a class="toggle-nav-collapse" href="#" title="Open/Close"></a>
    </div>
</div>