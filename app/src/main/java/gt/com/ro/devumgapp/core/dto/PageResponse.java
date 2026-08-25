package gt.com.ro.devumgapp.core.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Mirrors Spring's Page&lt;T&gt; JSON shape so any paginated endpoint can be
 * deserialized generically with Retrofit, e.g. Call&lt;PageResponse&lt;CarreraResponse&gt;&gt;.
 * Fields like "pageable" or "sort" are intentionally omitted: Gson ignores them.
 */
public class PageResponse<T> {

    @SerializedName("content")
    public List<T> content;

    @SerializedName("totalPages")
    public int totalPages;

    @SerializedName("totalElements")
    public long totalElements;

    @SerializedName("last")
    public boolean last;

    @SerializedName("size")
    public int size;

    @SerializedName("number")
    public int number;

    @SerializedName("first")
    public boolean first;

    @SerializedName("empty")
    public boolean empty;
}
