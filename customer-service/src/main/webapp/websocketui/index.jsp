<html>
<head>
<link rel="stylesheet" type="text/css" href="../style.css">
</head>
<body onLoad="load();">
	<h2>Dynamic UI (with Websockets)</h2>
	<div class="row">
		<div class="column">
			<form class="customerInput" action="" name="addForm">
				<fieldset>
					<legend>Add New Customer</legend>
					<div class="inputfield">
						<label for="fullName">Full Name:</label> <input type="text"
							name="fullName" id="fullName">
					</div>
					<div class="inputfield">
						<label for="phoneNumber">Phone Number:</label> <input type="text"
							name="phoneNumber" id="phoneNumber">
					</div>
					<div class="inputfield">
						<input type="button" onClick="addCustomer();" value="Add Customer">
					</div>
				</fieldset>
				<p class="message" id="printMessageContainer">
			</form>
		</div>
		<div class="column">
			<ul id="customers"></ul>
		</div>
	</div>
	<script>
	function createNode(element) {
		return document.createElement(element);
	}
	
	function append(parent, el) {
		return parent.appendChild(el);
	}

	function load() {
		print("Loading...");
		loadCustomers();
		setupSocket();
		print(" ");
	}
	
	function setupSocket() {
		print("Setting up Websocket...")
		var socket = new WebSocket("ws://" + window.location.host + "/customersocket");
		socket.onopen = function (event) {
			print("Opened websocket")
			socket.send("Testing");
		};

		socket.onmessage = function (event) {
			handleEvent(event);
		};
		
        socket.onerror = function(event) {
            console.log("onerror:" + JSON.stringify(event, null, 4));
        }
	}
	
	function handleEvent(event) {
		var json = JSON.parse(event.data);
		print ("Server Event: " + event.data);
		if (json.action == "add") {
			addCustomerToList(json);
		} else if (json.action == "remove") {
			removeCustomerFromList(json);
		} else if (json.action == "message") {
			print(json.text);
		} else {
			print("Received unsupported json message! " + json);
		}
	}
	
	function addCustomerToList(customer) {
		const ul = document.getElementById('customers');
		const li = document.getElementById("LI" + customer.id);
		if (li == null) {
	    	let li = document.createElement("li");
	    	li.id = "LI" + customer.id;
	    	var span = createNode('span');	    	
	    	const entry = customer.fullName + " (" + customer.phoneNumber + ") <a class=\"deleteLink\" onclick=\"deleteUser(\'" + customer.id + "'\);\">Delete</a>";
			span.innerHTML = entry;
			append(li, span);
			append(ul, li);	
		} else {
			print("Warning: Got add event for already existing customer: " + customer.id);			
		}
	}
	
	function removeCustomerFromList(customer) {
		deleteListItem("LI" + customer.id);		
	}
	
	function loadCustomers() {
		print("Loading initial customers...");
		const ul = document.getElementById('customers');
		ul.innerHTML = "";
		const url = '/rest/customers';
		
		fetch(url).then(function(response) {
			  response.text().then(function(data) {
			    var customers = JSON.parse(data)
			    for (const s of customers) {
			    	const customerId = s.id;			    	
			    	let li = document.createElement("li");
			    	li.id = "LI" + customerId;
			    	var span = createNode('span');	    	
			    	const entry = s.fullName + " (" + s.phoneNumber + ") <a class=\"deleteLink\" onclick=\"deleteUser(\'" + customerId + "'\);\">Delete</a>";
					span.innerHTML = entry;
					append(li, span);
					append(ul, li);	
				}
			});
		})
	}
	
	function deleteListItem(elementId) {
		console.log(elementId);
		const li = document.getElementById(elementId);
		const ul = document.getElementById('customers');
		if (li != null) {
			ul.removeChild(li)
		}
	}
	
	function deleteUser(id) {
		const url = '/rest/customers/' + id;
		fetch(url, {
			  method: 'delete',
			  headers: {'Content-Type': 'application/json'}
			});
	}
	
	function addCustomer() {
		const data = {fullName: document.getElementById('fullName').value, 
				      phoneNumber: document.getElementById('phoneNumber').value};
		console.log(data);
		const url = '/rest/customers/';
		fetch(url, {
			  method: 'put',
			  headers: {'Content-Type': 'application/json'},
			  body: JSON.stringify(data)
			})
		print("Sent put for " + data.fullName);
		clearInput();
	}
	
	function clearInput() {
		document.getElementById('fullName').value ="";
		document.getElementById('phoneNumber').value ="";		
	}

	function print(s) {
        var  p = document.getElementById('printMessageContainer')
        p.innerText = s;
    }
  </script>
</body>
</html>