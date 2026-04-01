.PHONY: all clean compile test package

all: compile test package

clean:
	mvn clean

compile:
	mvn compile

test:
	mvn test

package:
	mvn package
