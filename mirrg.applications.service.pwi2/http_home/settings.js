$(function() {
	$("#applicationName").text("mirrg.applications.service.pwi2");
	$("#paneTools").append($('<button>').addClass("tool").text("Auto Restart On").click(function() {
		send("auto_restart true");
	}));
	$("#paneTools").append($('<button>').addClass("tool").text("Auto Restart Off").click(function() {
		send("auto_restart false");
	}));
	$("#paneTools").append($('<button>').addClass("tool").text("Run Process").click(function() {
		send("start");
	}));
	$("#paneTools").append($('<button>').addClass("tool").text("Kill Process").click(function() {
		if (confirm("Do you want to terminate the process?")) {
			send("stop");
		}
	}));
	$("#paneTools").append($('<button>').addClass("tool").text("Exit").click(function() {
		if (confirm("Do you want to terminate the process manager?")) {
			send("exit");
		}
	}));
});
