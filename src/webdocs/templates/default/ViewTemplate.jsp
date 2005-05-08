<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname" />: <wiki:PageName /></title>
  <wiki:Include page="commonheader.jsp"/>
  <wiki:CheckVersion mode="notlatest">
        <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckVersion>
</head>

<body bgcolor="#FFFFFF">

<table border="0" cellspacing="8" width="95%">

  <tr>
    <td class="leftmenu" width="10%" valign="top" nowrap="nowrap">
       <wiki:Include page="LeftMenu.jsp"/>
       <p>
       <wiki:CheckRequestContext context="view">
          <wiki:Permission permission="edit">
             <wiki:EditLink>Edit this page</wiki:EditLink>
          </wiki:Permission>
       </wiki:CheckRequestContext>
       </p>
       <wiki:Include page="LeftMenuFooter.jsp"/>
       
       <!-- Strictly for debugging -->
       <p>Page: <wiki:PageName/></p>
       <p>You: <wiki:UserName/></p>
       <p>Permissions:</p>
       <ul>
           <wiki:Permission permission="view"><li>view</li></wiki:Permission>
           <wiki:Permission permission="comment"><li>comment</li></wiki:Permission>
           <wiki:Permission permission="edit"><li>edit</li></wiki:Permission>
           <wiki:Permission permission="upload"><li>upload</li></wiki:Permission>
           <wiki:Permission permission="rename"><li>rename</li></wiki:Permission>
           <wiki:Permission permission="delete"><li>delete</li></wiki:Permission>
           <wiki:Permission permission="registerUser"><li>register</li></wiki:Permission>
           <wiki:Permission permission="createPages"><li>create pages</li></wiki:Permission>
           <wiki:Permission permission="createGroups"><li>create groups</li></wiki:Permission>
       </ul>
       <p>Status: <wiki:Variable var="loginstatus"/></p>

       <br /><br />
       <div align="center">
           <wiki:RSSImageLink title="Aggregate the RSS feed" />
       </div>
    </td>

    <td class="page" width="85%" valign="top">

      <table width="100%" cellspacing="0" cellpadding="0" border="0">
         <tr>
            <td align="left">
                <h1 class="pagename"><a name="Top"><wiki:PageName/></a></h1>
            </td>
            <td align="right"><wiki:Include page="SearchBox.jsp"/></td>
         </tr>
         <tr>
            <td colspan="2" class="breadcrumbs">Your trail: <wiki:Breadcrumbs /></td>
         </tr>
      </table>

      <hr />

      <wiki:Content/>

    </td>
  </tr>

</table>

</body>

</html>

