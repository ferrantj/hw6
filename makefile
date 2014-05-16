JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	FullException.java \
	PaddedPrimitive.java \
	RandomGenerator.java \
	Fingerprint.java \
	RangeList.java \
	RangeLists.java \
	Dispatcher.java \
	PacketGenerator.java \
	PacketQueue.java \
	PacketWorker.java \
	ParallelDispatcher.java \
	Firewall.java \
	

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
