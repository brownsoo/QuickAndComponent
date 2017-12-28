package com.hansoolabs.and.view;

import android.view.View;

import com.hansoolabs.and.databinding.Section;

/**
 * Created by brownsoo on 2017. 8. 23..
 */

public class SectionIndexIndicator extends IndexIndicator {

    public SectionIndexIndicator(View frame, final Section<?> section) {
        super(frame, new Adapter() {
            @Override
            public int getTotalSize() {
                return section.getSize();
            }

            @Override
            public int getCurrentIndex(int adapterPosition, int totalAdapterSize) {
                int offset = section.getIndex();
                return Math.min(Math.max(adapterPosition - offset + 2, 1), totalAdapterSize);
            }

            @Override
            public boolean isVisible() {
                return section.isVisible();
            }
        });
    }
}
