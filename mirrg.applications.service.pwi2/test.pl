
local $SIG{HUP} = sub {
	print "HUP!", "\n";
	sleep(5);
	exit(0);
};

local $SIG{TERM} = sub {
	print "TERM!", "\n";
	sleep(5);
	exit(0);
};

local $SIG{INT} = sub {
	print "INT!", "\n";
	sleep(5);
	exit(0);
};

$| = 1;

map {
	print "Line\n";
} 1..100;

print "Argument: " . join(" ", @ARGV), "\n";
print "process started", "\n";

while (<STDIN>) {
	chomp $_;
	if ($_ eq "stop") {
		print "stopped", "\n";
		exit(0);
	}
	print $_, "\n";
	sleep(1);
	print $_, "\n";
}

print "stdin closed", "\n";
print "stopped", "\n";
