package com.redmart.skiinginsingapore.util;

import java.util.Comparator;

/**
 * Created by archie on 15/1/16.
 */
public class ArrayIndexComparator implements Comparator<Integer>
{
    private final Integer[] array;

    public ArrayIndexComparator(Integer[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        // Autounbox from Integer to int to use as array indexes
        return array[index2].compareTo(array[index1]);
    }
}

