line: __LINE__
// https://www.geeksforgeeks.org/c-program-to-merge-two-arrays/
// C program to merge two arrays into a new array using
// memcpy()
line: __LINE__
#include <stdio.h>
line: __LINE__
#include <string.h>
line: __LINE__
#include <stdlib.h>
line: __LINE__

int* mergeArrays(int arr1[], int n1, int arr2[], int n2) {

    // Resultant array to store merged array
    int *res = (int*)malloc(sizeof(int) * n1 * n2);

    // Copy elements of the first array
    memcpy(res, arr1, n1 * sizeof(int));

    // Copy elements of the second array
    memcpy(res + n1, arr2, n2 * sizeof(int));

    return res;
}

int main() {
    int arr1[] = {1, 3, 5};
    int arr2[] = {2, 4, 6};
    int n1 = sizeof(arr1) / sizeof(arr1[0]);
    int n2 = sizeof(arr2) / sizeof(arr2[0]);

    // Merge arr1 and arr2
    int* res = mergeArrays(arr1, n1, arr2, n2);

    for (int i = 0; i < n1 + n2; i++)
        printf("%d ", res[i]);

    return 0;
}
line: __LINE__