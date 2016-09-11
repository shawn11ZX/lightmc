package com.wooduan.lightmc.serializer.jobj;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.serializer.ApcMigration;

public class HackedObjectInputStream extends ObjectInputStream
{

    /**
     * Migration table. Holds old to new classes representation.
     */
    private static final Map<String, Class<?>> MIGRATION_MAP = new HashMap<String, Class<?>>();
    private static Field nameField;
    static
    {
        MIGRATION_MAP.put(ApcMigration.JOBJ_NAME, APC.class);
        try {
			nameField = ObjectStreamClass.class.getDeclaredField("name");
			nameField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
    }

    public HackedObjectInputStream(final InputStream stream) throws IOException
    {
        super(stream);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException
    {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        for (final String oldName : MIGRATION_MAP.keySet())
        {
            if (resultClassDescriptor.getName().equals(oldName))
            {
                String replacement = MIGRATION_MAP.get(oldName).getName();

                try
                {
                    nameField.set(resultClassDescriptor, replacement);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException(e);
                }

            }
        }

        return resultClassDescriptor;
    }
}
