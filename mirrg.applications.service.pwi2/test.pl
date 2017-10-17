
`echo a > tmp_start.txt`;

local $SIG{HUP} = sub {
	print "HUP!", "\n";
	`echo a > tmp_hup1.txt`;
	sleep(5);
	`echo a > tmp_hup2.txt`;
	exit(0);
};

local $SIG{TERM} = sub {
	print "TERM!", "\n";
	`echo a > tmp_term1.txt`;
	sleep(5);
	`echo a > tmp_term2.txt`;
	exit(0);
};

local $SIG{INT} = sub {
	print "INT!", "\n";
	`echo a > tmp_int1.txt`;
	sleep(5);
	`echo a > tmp_int2.txt`;
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
