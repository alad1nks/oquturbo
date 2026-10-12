/* Disposable CI host cleanup only. Compile against the actual macOS SDK. */
#if !defined(__APPLE__) || !defined(__aarch64__)
#error This helper requires macOS arm64
#endif
#include <TargetConditionals.h>
#if !TARGET_OS_OSX
#error This helper requires the macOS host SDK
#endif
#include <errno.h>
#include <mach/mach_time.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>

static mach_timebase_info_data_t timebase;

static double monotonic_seconds(void) {
    return (double)mach_absolute_time() * timebase.numer / timebase.denom / 1e9;
}

static int number(const char *text, double *value) {
    char *end;
    errno = 0;
    *value = strtod(text, &end);
    return !errno && end != text && *end == '\0' && isfinite(*value);
}

int main(int argc, char **argv) {
    if (geteuid() != 0 || mach_timebase_info(&timebase) != KERN_SUCCESS || !timebase.denom) {
        fprintf(stderr, "Root and a valid Mach clock are required\n");
        return 1;
    }
    int probe = argc == 2 && strcmp(argv[1], "--probe") == 0;
    double original = 0, anchor = 0;
    if (!probe && (argc != 4 || strcmp(argv[1], "--restore") != 0 ||
                   !number(argv[2], &original) || !number(argv[3], &anchor) ||
                   original <= 0 || original >= 4102444800.0 || anchor < 0 || anchor > 1e12)) {
        fprintf(stderr, "Expected --probe or --restore original-epoch monotonic-anchor\n");
        return 1;
    }
    /* All startup, argument parsing and timebase setup precede target calculation. */
    double started = monotonic_seconds();
    double target = original + started - anchor;
    int code = 0, saved_errno = 0;
    if (!probe) {
        if (started < anchor || started - anchor > 7200 || target <= 0 || target >= 4102444800.0) {
            fprintf(stderr, "Invalid restoration clock coordinates\n");
            return 1;
        }
        struct timeval value;
        value.tv_sec = (time_t)floor(target);
        value.tv_usec = (suseconds_t)((target - floor(target)) * 1e6);
        code = settimeofday(&value, NULL);
        saved_errno = code ? errno : 0;
    }
    double finished = monotonic_seconds();
    struct timeval actual;
    if (gettimeofday(&actual, NULL) != 0) {
        perror("gettimeofday");
        return 1;
    }
    printf("{\"mode\":\"%s\",\"startedMonotonic\":%.9f,\"finishedMonotonic\":%.9f,"
           "\"epoch\":%.6f,\"target\":%.6f,\"code\":%d,\"errno\":%d}\n",
           probe ? "probe" : "restore", started, finished,
           (double)actual.tv_sec + actual.tv_usec / 1e6, probe ? 0 : target, code, saved_errno);
    return code == 0 ? 0 : 1;
}
