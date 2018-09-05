<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="se.hirt.examples.problematicwebapp.data.Customer"%>
<%@page import="java.util.Collection"%>
<%@page import="se.hirt.examples.problematicwebapp.data.DataAccess"%>
<!-- JSTL would be nice. Anyone know how to get it to work with a simple maven dependency? -->
<html>
<head>
<link rel="stylesheet" type="text/css" href="../style.css">
</head>
<body>
	<h2>Simple UI</h2>
</body>
<%
	List<Customer> customers = new ArrayList<>(DataAccess.getAllCustomers());
%>
<table class="customerTable">
	<tr>
		<th class="customerTable">Customer ID</th>
		<th class="customerTable">Customer Name</th>
		<th class="customerTable">Customer Phone</th>
		<th class="customerTable">Action</th>
	</tr>
	<%
		for (Customer c : customers) {
	%>
	<tr>
		<td class="customerTable"><%=c.getId()%></td>
		<td class="customerTable"><%=c.getFullName()%></td>
		<td class="customerTable"><%=c.getPhoneNumber()%></td>
		<td class="customerTable"><a class="customerDeleteLink" href="/deletecustomer?id=<%=String.valueOf(c.getId())%>">Delete</a></td>
	</tr>
	<%
		}
	%>
</table>
<br/>
<div class="customerInput">
	<form action="/addcustomer" method="POST">
		<table>
			<tr>
				<td colspan="2"><b>Add New User</b></td>
			</tr>
			<tr>
				<td>Full Name:</td>
				<td><input type="text" name="fullName"></td>
			</tr>
			<tr>
				<td>Phone Number:</td>
				<td><input type="text" name="phoneNumber"></td>
			</tr>
			<tr>
				<td><input type="submit" value="Submit"></td>
			</tr>
		</table>
	</form>
</div>

</html>
