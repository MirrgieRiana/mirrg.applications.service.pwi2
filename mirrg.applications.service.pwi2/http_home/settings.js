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
		send("stop");
	}));
});
