JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Dispatcher.java \
	Fingerprint.java \
	Firewall.java \
	FullException.java \
	PacketGenerator.java \
	PacketQueue.java \
	PacketWorker.java \
	PaddedPrimitive.java \
	ParallelDispatcher.java \
	RandomGenerator.java \
	RangeList.java \
	RangeLists.java \
	

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
