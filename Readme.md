[![Build Status](https://travis-ci.org/PapenfussLab/gridss.svg?branch=master)](https://travis-ci.org/PapenfussLab/gridss)
[![Coverage Status](https://coveralls.io/repos/github/PapenfussLab/gridss/badge.svg?branch=master)](https://coveralls.io/github/PapenfussLab/gridss?branch=master)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)

# GRIDSS - the Genomic Rearrangement IDentification Software Suite

GRIDSS is a module software suite containing tools useful for the detection of genomic rearrangements. GRIDSS includes a genome-wide break-end assembler, as well as a structural variation caller for Illumina sequencing data. GRIDSS calls variants based on alignment-guided positional de Bruijn graph genome-wide break-end assembly, split read, and read pair evidence.

GRIDSS makes extensive use of the [standard tags defined by SAM specifications](http://samtools.github.io/hts-specs/SAMtags.pdf). Due to the modular design, any step (such as split read identification) can be replaced by another implementation that also outputs using the standard tags. It is hoped that GRIDSS can serve as an exemplar modular structural variant pipeline designed for interoperability with other tools.

If you have any trouble running GRIDSS, please raise an issue using the Issues tab above. Based on feedback from users, a user guide will be produced outlining common workflows, pitfalls, and use cases.

[Click here to download GRIDSS](https://github.com/PapenfussLab/gridss/releases)

Detailed documentation is being developed [here](https://github.com/PapenfussLab/gridss/wiki/GRIDSS-Documentation)

# Citation

For citing GRIDSS and for an overview of the GRIDSS algorithms, refer to our open access article: http://genome.cshlp.org/content/early/2017/11/02/gr.222109.117.abstract

Daniel L. Cameron, Jan Schröder, Jocelyn Sietsma Penington, Hongdo Do, Ramyar Molania, Alexander Dobrovic, Terence P. Speed and Anthony T. Papenfuss.
GRIDSS: sensitive and specific genomic rearrangement detection using positional de Bruijn graph assembly.
Genome Research, 2017
doi: 10.1101/gr.222109.117


# Pre-requisites

To run GRIDSS the following must be installed:

* java 1.8 or later
* R 3.5 or later
* sambamba
* bwa

# Running

Pre-compiled binaries are available at https://github.com/PapenfussLab/GRIDSS/releases.

GRIDSS is built using htsjdk, so is invoked in the same manner as Picard tools utilities. GRIDSS invokes an external alignment tool at multiple points during processing. By default this is bwa mem, but can be configured to use bowtie2 or another aligner.

## Example scripts

Example scripts can be found in the examples subdirectory of this repository.

* example/GRIDSS.sh: simple example script to run GRIDSS on a single sample
* example/somatic.sh: simple tumour/normal somatic variant calling
* example/somatic.R shows how the StructuralVariantAnnotation R package can be used to analyse GRIDSS variant calls and export variants of interest to other formats such as BEDPE
* example/separate.sh: example script showing how the GRIDSS 
* example/gridss_fully_integrated_fastq_to_vcf_pipeline.sh: example script showing how the two preprocessing sorting steps can be avoided if the GRIDSS preprocessing steps are fully integrated into a NGS pipeline
## FAQ

### Should I process each input BAM separately or together?

Wherever possible, samples should be processed together. Joint calling enables the detection of low allelic fraction SVs, as well as making the downstream analysis much easier (a single VCF with a breakdown of support per sample is much easier to deal with than multiple VCFs - the matching logic required to determine if two SVs are equivalent is non-trivial).

GRIDSS joint calling has been tested on up 12 samples with ~1000x aggregate coverage. If you have hundreds of samples, joint assembly will likely be computationally prohibitive and you will need to perform assembly in batches, them merge the results together.

### I encountered an error. What should I do?

* Check the bottom of this page for commonly encountered errors and their solutions

### How many threads should I use?

1-16 threads is recommended. Note that pre-processing is limited by htsjdk BAM parsing thus is not multi-threaded. Asynchronous I/O means preprocessing will use up to 200-300% CPU for a nominally single-threaded operation. Make sure you specify enough memory for the number of threads you specified.

### How much memory should I give GRIDSS?

At least 4GB + 2GB per thread. It is recommended to run GRIDSS with max heap memory (-Xmx) of 8GB for single-threaded operation
(WORKER_THREADS=1), 16GB for multi-core desktop operation, and 31GB for heavily multi-threaded
server operation. Note that due to Java's use of [Compressed Oops](http://docs.oracle.com/javase/7/docs/technotes/guides/vm/performance-enhancements-7.html#compressedOop), specifying a max heap size of between 32-48GB effectively reduces the memory available to GRIDSS so is strongly discouraged.

### Should I include alt contigs in the reference?

GRIDSS relies on the aligner to determine the mapping location and quality of assembly contigs and to identify split read from soft clipped reads. GRIDSS considers alignments with low mapping quality (default mapq <= 10) to not be uniquely aligned and treats them as unaligned. If alt contigs are included in an aligner that is not alt-aware then hits to sequences that are in both the primary reference contigs and the alt contigs will be given a low mapq by the aligner and will be treated as unaligned by GRIDSS. By default, GRIDSS uses bwa mem for alignment so by including the bwa alt contig definition file, reads from regions with alt homology will be preferentially aligned to the reference with a correspondingly improved mapq. Whether or not to include alt contigs depends on what sort of downstream analysis you intend to perform and how your intend to handle structural variants involving alt contigs. That said, if your reference genome includes alt contigs and a bwa alt contig definition file is available for your genome, you should use it.

## GRIDSS tools

GRIDSS takes a modular approach so each step of the GRIDSS pipeline can be run independently. The following data flow diagram gives an overview of the GRIDSS pipeline:

![GRIDSS data flow diagram](https://docs.google.com/drawings/d/1aXFBH0E9zmW4qztHIEliZfsLCHJa6_-l624Frq1X-Ms/pub?w=973&h=760)

#### CallVariants

This tool runs every step in the variant calling pipeline. For most users, this is the only tool that will need to be run.

#### CollectGridssMetrics

GRIDSS requires a number of input library metrics to be calculated. The metrics calculations programs are invoked in the same manner as Picard tools metrics. This program functions similarly to Picard tools CollectMultipleMetrics but, by default, only extracts the metrics required by GRIDSS.

#### CollectCigarMetrics

Collects metrics regarding the size and type of read alignment CIGAR elements.

#### CollectIdsvMetrics

Collects generic library-level metrics such as the read and read pair mapping rates.

#### CollectMapqMetrics

Collects metrics regarding distribution of read mapping quality scores.

#### CollectTagMetrics

Collects metrics regarding presence of SAM tags.

#### ExtractSVReads

Extracts the subset of reads that provide potential support for structural variation. These reads fall into one or more of the following classes:
* Reads aligned with an insertion or deletion in the read alignment
* Soft clipped reads
* Split reads (identified by the SA SAM Tag)
* Discordant read pairs
* Read pairs with only 1 read mapped
* Unmapped reads

#### SoftClipsToSplitReads

Identifies split reads by iterative realignment of soft clipped bases with a NGS aligner. By default, bwa mem is used for read alignment.
The GRIDSS pipeline runs this on all input files, as well as the breakend assembly contigs generated by AssembleBreakends.

#### AssembleBreakends

Generates breakend assemblies from the input reads. Breakend assemblies are written as synthetic soft clipped reads.
Each of these reads corresponds to a breakend assembly contig composed of reads anchored (either directly, or via the mate read)
to the location of the breakend assembly.

Breakend contigs composed entirely of discordant read pairs or reads with unmapped mates cannot be uniquely placed as there exists
an interval over which the contig is anchored. These alignments are written using a placeholder CIGAR alignment of the form XNX.
For example, a breakend contig read with an alignment of 1X50N1X150S represents a 150bp contig in which the breakend is expected to
occur at one of the 52 genomic positions given by the placeholder XNX alignment interval.

Breakpoints are identified by running SoftClipsToSplitReads on the breakend assembly contigs. High quality breakpoints are expected
to have two independent breakend assemblies (one from each breakend of the breakpoint).

#### IdentifyVariants

Identifies putative structural variants from the reads providing potential SV support, and the breakend assembly contigs.

#### AnnotateVariants

Annotates breakpoint calls performing AllocateEvidence, AnnotateReferenceCoverage, AnnotateInexactHomology annotation.

##### AllocateEvidence

Uniquely allocate reads/read pairs to identified breakpoints by ensuring that for each read/read pair, only the single
best alignment is retained (relevant for input files containing multiple mapping locations for each read).
Read counts and other summary statistics are collated.

##### AnnotateReferenceCoverage

Calculates the number of reads and read pairs providing support for the reference allele at each breakpoint.

#### AnnotateInexactHomology

Calculates the size of the inexact homology between the reference sequence and the breakpoint sequence. Breakpoints
with long inexact homology are possibly due to alignment artifacts causing false positive breakpoint calls between
regions of homologous sequence.

## Common Parameters

GRIDSS programs have a large number of parameters that can be be adjusted. The default parameter set has been tested with paired-end Illumina data ranging from 2x36bp to 2x250bp and should give a reasonably good result. Command line used parameters are listed below.

### OUTPUT (Required)

Variant calling output file. Can be VCF or BCF.

### REFERENCE_SEQUENCE (Required)

Reference genome FASTA file. GRIDSS requires that the reference genome supplied exactly matches
the reference genome of all input files.
The reference genome must be in FASTA format and must have a tabix (.fai) index and an
index for the NGS aligner (by default bwa). The NGS aligner index prefix must match
the reference genome filename. For example, using the default setting against the reference
file reference.fa, the following files must be present and readable:

File | Description
------- | ---------
reference.fa | reference genome
reference.fa.fai | Tabix index
reference.fa.amb | bwa index
reference.fa.ann | bwa index
reference.fa.bwt | bwa index
reference.fa.pac | bwa index
reference.fa.sa | bwa index

These can be created using `samtools faidx reference.fa` and `bwa index reference.fa`

A .dict sequence dictionary is also required but GRIDSS will automatically create one if not found.

### INPUT (Required)

Input libraries. Specify multiple times (i.e. `INPUT=file1.bam INPUT=file2.bam INPUT=file3.bam`) to process multiple libraries together.

Input files must be coordinate sorted SAM/BAM/CRAM files.

GRIDSS considers all reads in each file to come from a single library.

Input files containing read groups from multiple different libraries should be split into an input file per-library.

The reference genome used for all input files should match the reference genome supplied to GRIDSS.

### INPUT_LABEL

Labels to allocate inputs. The default label for each input file corresponds to the file name but can be overridden by
specifying an INPUT_LABEL for each INPUT. The output for any INPUT files with the same INPUT_LABEL will be merged.

### ASSEMBLY (Required)

File to write breakend assemblies to. It is strongly recommended that the assembly filename corresponds to the OUTPUT filename. Using ASSEMBLY=assembly.bam is problematic as (like the INPUT files) the assembly file is relative not to WORKING_DIR, but to the current directory of the calling process. This is likely to result in data corruption when the same assembly file name is used on different data sets (for example, writing assembly.bam to your home directory when running on a cluster).

### BLACKLIST

BED blacklist of regions to exclude from analysis. The [ENCODE DAC blacklist](https://www.encodeproject.org/annotations/ENCSR636HFF/)
is recommended when aligning against hg19.

Unlike haplotype assemblers such as TIGRA and GATK, GRIDSS does not abort assembly when complex assembly graphs are encountered. Processing of these graphs slows down the assembly process considerably, so if regions such as telomeric and centromeric regions are to be excluded from downstream analysis anyway, assembly of these regions is not required. It is recommended that a blacklist such as the [ENCODE DAC blacklist](https://www.encodeproject.org/annotations/ENCSR636HFF/) be used to filter such regions. Inclusion of additional mappability-based blacklists is not required as GRIDSS already considers the read mapping quality.

### READ_PAIR_CONCORDANT_PERCENT

Portion (0.0-1.0) of read pairs to be considered concordant. Concordant read pairs are considered to provide no support for structural variation.
Clearing this value will cause GRIDSS to use the 0x02 proper pair SAM flag written by the aligner to determine concordant pairing.
Note that some aligners set this flag in a manner inappropriate for SV calling and set the flag for all reads with the expected orientation and strand regardless of the inferred fragment size.

### INPUT_MIN_FRAGMENT_SIZE, INPUT_MAX_FRAGMENT_SIZE

Per input overrides for explicitly specifying fragment size interval to be considered concordant. As with INPUT_LABEL, these must be specified
for all input files. Use null to indicate an override is not required for a particular input (e.g.
`INPUT=autocalc.bam INPUT_MIN_FRAGMENT_SIZE=null INPUT_MAX_FRAGMENT_SIZE=null INPUT=manual.bam INPUT_MIN_FRAGMENT_SIZE=100 INPUT_MAX_FRAGMENT_SIZE=300` )

### WORKER_THREADS

Number of processing threads to use, including number of threads to use when invoking the aligner.
Note that the number of threads spawned by GRIDSS is greater than the number of worker threads due to asynchronous I/O threads thus it is not uncommon to see over 100% CPU usage when WORKER_THREADS=1 as bam compression/decompression is a computationally expensive operation.
This parameter defaults to the number of cores available.

### WORKING_DIR

Directory to write intermediate results directories. By default, intermediate files for each input or output file are written to a subdirectory in the same directory as the relevant input or output file.
If WORKING_DIR is set, all intermediate results are written to subdirectories of the given directory.

### TMP_DIR

This field is a standard Picard tools argument and carries the usual meaning. Temporary files created during processes such as sort are written to this directory.

### samjdk defines

GRIDSS uses [htsjdk](https://github.com/samtools/htsjdk) as a SAM/BAM/CRAM/VCF parsing library. The following htsjdk java command-line options are strongly recommended for improved performance:

* -Dsamjdk.use_async_io_read_samtools=true
* -Dsamjdk.use_async_io_write_samtools=true
* -Dsamjdk.use_async_io_write_tribble=true

## libsswjni.so

Due to relatively poor performance of existing Java-based Smith-Waterman alignment packages, GRIDSS incorporates a JNI wrapper to the striped Smith-Waterman alignment library [SSW](https://github.com/mengyao/Complete-Striped-Smith-Waterman-Library). GRIDSS will attempt to load a precompiled version which is supplied as part of the GRIDSS package (a libsswjni.so file will be created in the TMP_DIR when GRIDSS is run). If the precompiled version is not compatible with your linux distribution, or you are running a different operating system, recompilation of the wrapper from source will be required. When recompiling, ensure the correct libsswjni.so is loaded using -Djava.library.path, or the LD_LIBRARY_PATH environment variable as per the JNI documentation.

If you have an older CPU that does not support SSE instructions, GRIDSS will terminate with a fatal error when loading the library. Library loading can be disabled by adding `-Dsswjni.disable=true` to the GRIDSS command line. If libsswjni.so cannot be loaded, GRIDSS will fall back to a (50x) slower java implementation which will result in the GRIDSS inexact homology variant annotation step running very slowly.

### CONFIGURATION_FILE

GRIDSS uses a large number of configurable settings and thresholds which for ease of use are not included
as command line arguments. Any of these individual settings can be overriden by specifying a configuration
file to use instead. Note that this configuration file uses a different format to the Picard tools-compatible
configuration file that is used instead of the standard command-line arguments.

When supplying a custom configuration, GRIDSS will use the overriding settings for all properties specified
and fall back to the default for all properties that have not been overridden. Details on the meaning
of each parameter can be found in the javadoc documentation of the au.edu.wehi.idsv.configuration classes.

# Output

GRIDSS is fundamentally a structural variation breakpoint caller. Variants are output as VCF breakends. Each call is a breakpoint consisting of two breakends, one from location A to location B, and a reciprocal record from location B back to A. Note that although each record fully defines the call, the VCF format requires both breakends to be written as separate records.

To assist in downstream analysis, the StructuralVariantAnnotation R package (https://github.com/PapenfussLab/StructuralVariantAnnotation) is strongly recommended. Operations such as variant filter, annotation and exporting to other formats such as BEDPE can be easily accomplished using this package in conjuction with the BioConductor annotation packages.

## Quality score

GRIDSS calculates quality scores according to the model outlined in the [paper](http://biorxiv.org/content/early/2017/02/21/110387).
As GRIDSS does not yet perform multiple test correction or score recalibration, QUAL scores are vastly overestimated for all variants.
As a rule of thumb, variants that have QUAL >= 1000 and have assemblies from both sides of the breakpoint (AS > 0 & RAS > 0) are considered of high quality,
variants with QUAL >= 500 but that can only be assembled from one breakend (AS > 0 | RAS > 0) are considered of intermediate quality,
and variants with low QUAL score or lack any supporting assemblies are considered to be of low quality.

## Non-standard INFO fields

GRIDSS writes a number of non-standard VCF fields. These fields are described in the VCF header.

## BEDPE

GRIDSS supports conversion of VCF to BEDPE format using the VcfBreakendToBedpe utility program included in the GRIDSS jar.
A working example of this conversion utility is provided in example/GRIDSS.sh

Calling VcfBreakendToBedpe with `INCLUDE_HEADER=true` will include a header containing column names in the BEDPE file.
These fields match the VCF INFO fields of the same name.
For BEDPE output, breakend information is not exported and per category totals (such as split read counts) are aggregated to a single value.

## Visualisation of results

When performing downstream analysis on variant calls, it can be immensely useful to be able to inspect
the reads that the variant caller used to make the variant calls. As part of the GRIDSS pipeline, the following
intermediate files are generated:

* *INPUT*.sv.bam
* *ASSEMBLY*.sv.bam

The inputsv  file contains all reads GRIDSS considered as providing putative support for
any potential breakpoint, including breakpoints of such low quality that GRIDSS did not
make any call. This file includes all soft clipped, indel-containing, and split reads, as
well as all discordant read pairs and pairs with only one read mapped.

Split reads can be identified by the presence of a
[SA SAM tag](http://samtools.github.io/hts-specs/SAMtags.pdf).

GRIDSS treats breakend assemblies as synthetic soft clipped read alignments thus assemblies
are displayed in the same manner as soft clipped/split reads.

* No SA tags indicates a breakpoint could not be unambiguously identified from the breakend contig.
* The source of the breakend contig can be identified by the read without the "Supplementary alignment" 0x800 SAM flag set.
* More than two alignments for a breakend contig indicates the contig spans a complex event involving multiple breakpoints.

## Intermediate Files

GRIDSS writes a large number of intermediate files. If rerunning GRIDSS with different parameters on the same input, all intermediate files must be deleted, or a different WORKING_DIR specified. All intermediate files are written to the WORKING_DIR directory tree, with the exception of temporary sort buffers which are written to TMP_DIR and automatically deleted at the conclusion of the sort operation.

File | Description
------- | ---------
gridss.* | Temporary intermediate file
gridss.lock.breakend.vcf | Lock directory to ensure that only one instance of GRIDSS is running for any given output file.
*.bai | BAM index for coordinate sorted intermediate BAM file.
WORKING_DIR/*file*.gridss.working | Working directory for intermediate files related to the given file.
WORKING_DIR/*input*.gridss.working/*input*\*_metrics | Various summary metrics for the given input file.
WORKING_DIR/*input*.gridss.working/*input*.realign.*N*.fq | Split read identification fastq file requiring alignment by NGS aligner.
WORKING_DIR/*input*.gridss.working/*input*.realign.*N*.bam | Result of NGS alignment.
WORKING_DIR/*input*.gridss.working/*input*.sv.bam | Subset of input reads considered by GRIDSS. This file is useful for visualisation of the supporting reads contributing to structural variant calls. Note that this file includes split read alignments identified from soft clipped reads in the input file.
WORKING_DIR/*assembly*.gridss.working/*assembly*.sv.bam | Assembly contigs represented as soft clipped or split reads. These are the assembly contigs GRIDSS uses for variant calling. Note that, as with split reads, GRIDSS uses the SA tag to encode split read alignments.
WORKING_DIR/*output*.gridss.working/*output*.breakpoint.vcf | Raw unannotated variant calls before unique allocation of multi-mapping reads.

## Building from source

Maven is used for build and dependency management which simplifies compile to the following steps:

* `git clone https://github.com/PapenfussLab/gridss`
* `cd gridss`
* `mvn clean package`

If GRIDSS was built successfully, a combined jar containing GRIDSS and all required libraries located at target/GRIDSS-_VERSION_-gridss-jar-with-dependencies.jar will have been created.

# Error Messages

For some error messages, it is difficult to determine the root cause and what to do to fix it.
Here is a list of key phrases of errors encountered by users and their solution

### Aborting since lock gridss.lock._OUTPUT_ already exists. GRIDSS does not support multiple simulatenous instances running on the same data.

Multiple instances of GRIDSS were run on the same data. GRIDSS does not yet support MPI parallelisation across multiple machines. Use the WORKER_THREADS parameter to specify the desired level of multi-threading. If using a cluster/job queuing system, a single non-MPI job should be submitted and either WORKER_THREADS explicitly set to the number of cores associated with the job requests, or the job should request the entire node.

If the lock directory exists and you know a GRIDSS process is not running (eg: the GRIDSS process was killed), then you can safely delete the lock directory.

### Exception in thread "main" java.lang.UnsupportedClassVersionError: au/edu/wehi/idsv/Idsv : Unsupported major.minor version 52.0

You are attempting to run GRIDSS with an old Java version. GRIDSS requires Java 8 or later.

### ExternalProcessFastqAligner	Subprocess terminated with with exit status 1. Alignment failed for _INPUT_.realign.0.fq

The external aligner (bwa) could not be run. The most common causes of this are:
- bwa is not on `PATH`
 - Does running "bwa" print out the bwa usage message? If you are using a cluster, you may have to add bwa to your `PATH` (eg `module add bwa`).
- bwa index does not exist
- bwa index has incorrect suffix
 - e.g. if the reference is ref.fa the index must be ref.fa.bwt _not_ ref.bwt

Can you run the bwa command exactly as it appears in the error message?

###  (Too many open files)

GRIDSS has attempted to open too many files at once and the OS file handle limit has been reached.
On linux 'ulimit -n' displays your current limit. This error likely to be encountered if you have specified a large number of input files or threads. The following solution is recommended:
* Increase your OS limit on open file handles (eg `ulimit -n _<larger number>_`)
  * Note that many linux systems have a default hard limit on open file handles of 4096 which with many samples is frequently too still too few. Increasing the hard limit requires root access.
* Added `-Dgridss.defensiveGC=true` to the java command-line used for GRIDSS. Memory mapped file handles are not released to the OS until the buffer is garbage collected . This option add a request forr garbage collection whenever a file handle is no longer used.

If those options fail, your remaining options are:
* Reduce number of worker threads. A large number of input files being processed in parallel results in a large number of files open at the same time.
* Increase the chunk size. The default chunk size is 10 million bases. This can be increased by adding a `chunkSize=100000000` line a `gridss.properties` file and adding `CONFIGURATION_FILE=gridss.properties` to the GRIDSS command line. Note that this will increase the number of bases processed by each job thus reduce the level of parallelisation possible.
* As a last-ditch effort, you can keep rerunning GRIDSS until it completes. If you are using the default entry point of `gridss.CallVariants` and have `-Dgridss.gridss.output_to_temp_file=true`, then you can rerun GRIDSS and it will continue from where it left off. Assuming it doesn't keep dying at the same spot, it will eventually complete.

### Reference genome used by _input.bam_ does not match reference genome _reference.fa_. The reference supplied must match the reference used for every input.

The reference genome used to align input.bam does not match the reference genome supplied to GRIDSS.
If the differences are purely based on chromosome name and ordering, the Picard tools utility ReorderBam
can be used to fix chromosome orderings.

### Unable to use sswjni library - assembly will be very slow. Please ensure libsswjni for your OS and architecture can be found on java.library.path

The sswjni library could not be loaded as the precompiled version is not compatable with your environment. See the sswjni sections for details on how to disable libsswjni or recompile it for your system.

### "Segmentation Fault", fatal JVM error, or no error message.

This is likely to be caused by a crash during alignment in libsswjni. See the sswjni sections for details on how to disable libsswjni or recompile it for your system.

### Illegal Instruction

Your CPU does not support the SSE2 instruction set. See the sswjni sections for details on how to disable libsswjni.

### Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x00007fc36e200000, 48234496, 0) failed; error='Cannot allocate memory' (errno=12)

GRIDSS has run out of memory. Either not enough memory has been allocated to run GRIDSS or GRIDSS has attempted to memory map too many files (See "(Too many open files)"). In both cases, restart GRIDSS (increasing the memory available if required) and GRIDSS will continue from where it left off.

### java.lang.AssertionError: java.lang.ClassNotFoundException: com.sun.tools.javac.api.JavacTool

You are running GRIDSS in multi-mapping mode using only a JRE instead of a full JDK. Update your PATH and JAVA_HOME to a Java 1.8+ JDK installation.

### htsjdk.samtools.util.RuntimeIOException: java.io.IOException: No space left on device

Just like Picard tools and htsjdk libraries that GRIDSS uses, intermediate files are sorted according the the `TEMP` file location. On many system, /tmp does not have enough space to sort a BAM file so it is possible to run out of intermediate file storage even if you have plenty of space left on the file system the input and output files are stored on. Using the same command-line options as Picard tools, the intermediate files location can be set using the `TMP_DIR` command-line argument.

It's also possible that you've just run out of space.





